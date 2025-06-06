package com.user.demo.service;

/**
 * Service for user management, including bulk operations, permission updates, and validation.
 */
import com.user.demo.dto.UserBulkRequest;
import com.user.demo.dto.UserRequest;
import com.user.demo.dto.UserUpdateRequest;
import com.user.demo.exception.BusinessException;
import com.user.demo.model.User;
import com.user.demo.model.LocationPermission;
import com.user.demo.repository.UserRepository;
import com.user.demo.repository.LocationPermissionRepository;
import com.user.demo.repository.AgentRepository;
import com.user.demo.repository.FieldRepository;
import com.user.demo.repository.NationRepository;
import com.user.demo.repository.AreaRepository;
import com.user.demo.repository.ProvinceRepository;
import com.user.demo.repository.DistrictRepository;
import com.user.demo.repository.MainStationRepository;
import com.user.demo.repository.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LocationPermissionRepository locationPermissionRepository;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private FieldRepository fieldRepository;

    @Autowired
    private NationRepository nationRepository;

    @Autowired
    private AreaRepository areaRepository;

    @Autowired
    private ProvinceRepository provinceRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private MainStationRepository mainStationRepository;

    @Autowired
    private StationRepository stationRepository;

    /**
     * Bulk create or update users, and optionally delete non-existent users.
     */
    @Transactional
    public void upsertUsers(UserBulkRequest request) {
        log.info("Starting bulk user upsert with request: {}", request);
        List<UserRequest> userRequests = request.getData();
        Set<String> usernamesInRequest = new HashSet<>();

        // First, process all updates and creates
        for (UserRequest userReq : userRequests) {
            log.info("Processing user request: {}", userReq);
            if (userReq.getEmail() == null || userReq.getEmail().trim().isEmpty()) {
                log.warn("Skipping user with null or empty email");
                continue;
            }
            Optional<String> usernameOpt = extractUsernameFromEmail(userReq.getEmail());
            if (usernameOpt.isEmpty()) {
                log.warn("Skipping user with invalid email: {}", userReq.getEmail());
                continue;
            }
            String username = usernameOpt.get();
            log.info("Generated username: {}", username);
            usernamesInRequest.add(username);
            
            try {
                User user = userRepository.findById(username).orElseGet(User::new);
                log.info("Found existing user: {}", user);
                user.setUsername(username);
                user.setEmail(userReq.getEmail());
                user.setEmployeeId(userReq.getEmployeeId());
                user.setFullname(userReq.getFullname());
                user.setPhoneNumber(userReq.getPhoneNumber());
                user.setBirthYear(userReq.getBirthYear());
                user.setPosition(userReq.getPosition());
                user.setDepartment(userReq.getDepartment());
                user.setIsAllowed(false);
                user.setAgentPermission("");
                user.setFieldPermission("");

                // Create or update location permission
                LocationPermission locationPermission = user.getLocationPermission();
                if (locationPermission == null) {
                    locationPermission = new LocationPermission();
                    locationPermission.setUsername(username);
                    locationPermission.setUser(user);
                    user.setLocationPermission(locationPermission);
                }

                userRepository.save(user);
                log.info("Saved user with location permission: {}", user);
            } catch (Exception e) {
                log.error("Error processing user {}: {}", username, e.getMessage(), e);
                throw new BusinessException("PROCESSING_ERROR", "Error processing user " + username + ": " + e.getMessage());
            }
        }

        // Then, handle deletions if requested
        if (request.isDeleteNonExistPeople()) {
            log.info("Handling deletion of non-existent users");
            try {
                List<User> usersToDelete = userRepository.findAll().stream()
                    .filter(user -> !usernamesInRequest.contains(user.getUsername()))
                    .collect(Collectors.toList());

                if (!usersToDelete.isEmpty()) {
                    userRepository.deleteAll(usersToDelete);
                    log.info("Deleted {} users and their permissions in batch", usersToDelete.size());
                }
            } catch (Exception e) {
                log.error("Error deleting non-existent users: {}", e.getMessage(), e);
                throw new BusinessException("DELETE_ERROR", "Failed to delete non-existent users: " + e.getMessage());
            }
        }
        log.info("Bulk user upsert completed successfully");
    }

    private Optional<String> extractUsernameFromEmail(String email) {
        if (email == null) return Optional.empty();
        int atIdx = email.indexOf('@');
        return atIdx > 0 ? Optional.of(email.substring(0, atIdx)) : Optional.of(email);
    }

    /**
     * Update user permissions and location permissions.
     */
    @Transactional
    public void updateUsers(UserUpdateRequest request) {
        log.info("updateUsers called with data: {}", request.getData());
        if (request.getData() == null || request.getData().isEmpty()) {
            log.warn("Request data is null or empty");
            throw new BusinessException("INVALID_REQUEST", "Request data cannot be empty");
        }

        // Get all usernames first
        Set<String> usernames = request.getData().stream()
            .map(UserUpdateRequest.UserData::getUsername)
            .collect(Collectors.toSet());

        // Get all users in one query
        Map<String, User> existingUsers = userRepository.findAllById(usernames)
            .stream()
            .collect(Collectors.toMap(User::getUsername, user -> user));

        // Process updates
        for (UserUpdateRequest.UserData userData : request.getData()) {
            log.info("Processing user: {}", userData.getUsername());
            validateUserData(userData);

            User user = existingUsers.get(userData.getUsername());
            if (user == null) {
                throw new BusinessException("USER_NOT_FOUND", "User " + userData.getUsername() + " not found");
            }

            // Update user permissions
            user.setIsAllowed(userData.isAllowed());

            if (!userData.isAllowed()) {
                // Format 1: Disable user
                user.setAgentPermission("");
                user.setFieldPermission("");
                user.setApprovedAt(null);

                // Clear location permission
                LocationPermission locationPermission = user.getLocationPermission();
                if (locationPermission != null) {
                    locationPermission.setNation(null);
                    locationPermission.setArea(null);
                    locationPermission.setProvince(null);
                    locationPermission.setDistrict(null);
                    locationPermission.setMainStation(null);
                    locationPermission.setStation(null);
                }
            } else {
                // Format 2: Enable user with permissions
                user.setAgentPermission(String.join(",", userData.getAgent()));
                user.setFieldPermission(String.join(",", userData.getField()));
                user.setApprovedAt(LocalDateTime.now());

                // Update location permission
                LocationPermission locationPermission = user.getLocationPermission();
                if (locationPermission == null) {
                    locationPermission = new LocationPermission();
                    locationPermission.setUsername(user.getUsername());
                    locationPermission.setUser(user);
                    user.setLocationPermission(locationPermission);
                }

                // Set the specific location field based on level
                String level = userData.getLocationPermission().getLevel();
                String value = userData.getLocationPermission().getValue();
                
                switch (level.toLowerCase()) {
                    case "nation":
                        locationPermission.setNation(value);
                        locationPermission.setArea(null);
                        locationPermission.setProvince(null);
                        locationPermission.setDistrict(null);
                        locationPermission.setMainStation(null);
                        locationPermission.setStation(null);
                        break;
                    case "area":
                        locationPermission.setArea(value);
                        locationPermission.setProvince(null);
                        locationPermission.setDistrict(null);
                        locationPermission.setMainStation(null);
                        locationPermission.setStation(null);
                        break;
                    case "province":
                        locationPermission.setProvince(value);
                        locationPermission.setDistrict(null);
                        locationPermission.setMainStation(null);
                        locationPermission.setStation(null);
                        break;
                    case "district":
                        locationPermission.setDistrict(value);
                        locationPermission.setMainStation(null);
                        locationPermission.setStation(null);
                        break;
                    case "main_station":
                        locationPermission.setMainStation(value);
                        locationPermission.setStation(null);
                        break;
                    case "station":
                        locationPermission.setStation(value);
                        break;
                }
            }

            userRepository.save(user);
            log.info("Updated user and location permission: {}", user);
        }
    }

    private void validateUserData(UserUpdateRequest.UserData userData) {
        if (userData.getUsername() == null || userData.getUsername().trim().isEmpty()) {
            throw new BusinessException("INVALID_USERNAME", "Username cannot be empty");
        }

        if (userData.isAllowed()) {
            // Only validate these fields if user is being enabled
            if (userData.getAgent() == null || userData.getAgent().isEmpty()) {
                throw new BusinessException("INVALID_AGENT", "Agent list cannot be empty");
            }
            if (userData.getField() == null || userData.getField().isEmpty()) {
                throw new BusinessException("INVALID_FIELD", "Field list cannot be empty");
            }

            // Validate agent codes
            for (String agentCode : userData.getAgent()) {
                try {
                    Long agentId = Long.parseLong(agentCode);
                    if (!agentRepository.existsById(agentId)) {
                        throw new BusinessException("INVALID_AGENT", "Agent code " + agentCode + " does not exist");
                    }
                } catch (NumberFormatException e) {
                    throw new BusinessException("INVALID_AGENT", "Agent code " + agentCode + " is not a valid number");
                }
            }

            // Validate field codes
            for (String fieldCode : userData.getField()) {
                try {
                    Long fieldId = Long.parseLong(fieldCode);
                    if (!fieldRepository.existsById(fieldId)) {
                        throw new BusinessException("INVALID_FIELD", "Field code " + fieldCode + " does not exist");
                    }
                } catch (NumberFormatException e) {
                    throw new BusinessException("INVALID_FIELD", "Field code " + fieldCode + " is not a valid number");
                }
            }

            if (userData.getLocationPermission() == null) {
                throw new BusinessException("INVALID_LOCATION", "Location permission cannot be null");
            }
            if (!isValidLocationLevel(userData.getLocationPermission().getLevel())) {
                throw new BusinessException("INVALID_LEVEL", "Invalid location level: " + userData.getLocationPermission().getLevel());
            }

            // Validate if location value exists in the corresponding table
            String level = userData.getLocationPermission().getLevel();
            String value = userData.getLocationPermission().getValue();
            
            switch (level.toLowerCase()) {
                case "nation":
                    if (!nationRepository.existsById(value)) {
                        throw new BusinessException("INVALID_NATION", "Nation code " + value + " does not exist");
                    }
                    break;
                case "area":
                    if (!areaRepository.existsById(value)) {
                        throw new BusinessException("INVALID_AREA", "Area code " + value + " does not exist");
                    }
                    break;
                case "province":
                    if (!provinceRepository.existsById(value)) {
                        throw new BusinessException("INVALID_PROVINCE", "Province code " + value + " does not exist");
                    }
                    break;
                case "district":
                    if (!districtRepository.existsById(value)) {
                        throw new BusinessException("INVALID_DISTRICT", "District code " + value + " does not exist");
                    }
                    break;
                case "main_station":
                    if (!mainStationRepository.existsById(value)) {
                        throw new BusinessException("INVALID_MAIN_STATION", "Main station code " + value + " does not exist");
                    }
                    break;
                case "station":
                    if (!stationRepository.existsById(value)) {
                        throw new BusinessException("INVALID_STATION", "Station code " + value + " does not exist");
                    }
                    break;
            }
        }
    }

    private boolean isValidLocationLevel(String level) {
        return level != null && Arrays.asList("nation", "area", "province", "district", "main_station", "station")
            .contains(level.toLowerCase());
    }
} 