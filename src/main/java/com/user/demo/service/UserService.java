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
import org.springframework.cache.annotation.CacheEvict;
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
    @CacheEvict(value = {"users", "permissions"}, allEntries = true)
    public void upsertUsers(UserBulkRequest request) {
        log.info("Starting bulk user upsert with request: {}", request);
        
        // Extract and validate all usernames first
        Map<String, UserRequest> validUserRequests = extractValidUserRequests(request.getData());
        if (validUserRequests.isEmpty()) {
            log.warn("No valid user requests found");
            return;
        }

        // Create all users in memory
        List<User> usersToSave = createUsersFromRequests(validUserRequests);
        
        // Save all users in one batch
        if (!usersToSave.isEmpty()) {
            userRepository.saveAll(usersToSave);
            log.info("Saved {} users in batch", usersToSave.size());
        }

        // Handle deletions if requested
        if (request.isDeleteNonExistPeople()) {
            handleUserDeletions(validUserRequests.keySet());
        }
        
        log.info("Bulk user upsert completed successfully");
    }

    private Map<String, UserRequest> extractValidUserRequests(List<UserRequest> requests) {
        Map<String, UserRequest> validRequests = new HashMap<>();
        
        for (UserRequest userReq : requests) {
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
            validRequests.put(username, userReq);
        }
        
        return validRequests;
    }

    private List<User> createUsersFromRequests(Map<String, UserRequest> validRequests) {
        List<User> users = new ArrayList<>();
        
        for (Map.Entry<String, UserRequest> entry : validRequests.entrySet()) {
            String username = entry.getKey();
            UserRequest userReq = entry.getValue();
            
            try {
                User user = new User();
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

                // Create location permission
                LocationPermission locationPermission = new LocationPermission();
                locationPermission.setUsername(username);
                locationPermission.setUser(user);
                user.setLocationPermission(locationPermission);

                users.add(user);
            } catch (Exception e) {
                log.error("Error creating user {}: {}", username, e.getMessage(), e);
                throw new BusinessException("PROCESSING_ERROR", "Error creating user " + username + ": " + e.getMessage());
            }
        }
        
        return users;
    }

    private void handleUserDeletions(Set<String> validUsernames) {
        log.info("Handling deletion of non-existent users");
        try {
            List<User> usersToDelete = userRepository.findAll().stream()
                .filter(user -> !validUsernames.contains(user.getUsername()))
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

    private Optional<String> extractUsernameFromEmail(String email) {
        if (email == null) return Optional.empty();
        int atIdx = email.indexOf('@');
        return atIdx > 0 ? Optional.of(email.substring(0, atIdx)) : Optional.of(email);
    }

    /**
     * Update user permissions and location permissions.
     */
    @Transactional
    @CacheEvict(value = {"users", "permissions"}, allEntries = true)
    public void updateUsers(UserUpdateRequest request) {
        log.info("updateUsers called with data: {}", request.getData());
        if (request.getData() == null || request.getData().isEmpty()) {
            log.warn("Request data is null or empty");
            throw new BusinessException("INVALID_REQUEST", "Request data cannot be empty");
        }

        // Extract and validate all usernames first
        Map<String, UserUpdateRequest.UserData> validUserData = extractValidUserData(request.getData());
        if (validUserData.isEmpty()) {
            log.warn("No valid user data found");
            return;
        }

        // Pre-fetch all required data
        Map<String, User> existingUsers = userRepository.findAllById(validUserData.keySet())
            .stream()
            .collect(Collectors.toMap(User::getUsername, user -> user));

        // Process updates in batches
        List<User> usersToUpdate = new ArrayList<>();
        for (Map.Entry<String, UserUpdateRequest.UserData> entry : validUserData.entrySet()) {
            String username = entry.getKey();
            UserUpdateRequest.UserData userData = entry.getValue();
            
            log.info("Processing user: {}", username);
            validateUserData(userData);

            User user = existingUsers.get(username);
            if (user == null) {
                throw new BusinessException("USER_NOT_FOUND", "User " + username + " not found");
            }

            updateUserWithData(user, userData);
            usersToUpdate.add(user);
        }

        // Save all updates in one batch
        if (!usersToUpdate.isEmpty()) {
            userRepository.saveAll(usersToUpdate);
            log.info("Updated {} users in batch", usersToUpdate.size());
        }
    }

    private Map<String, UserUpdateRequest.UserData> extractValidUserData(List<UserUpdateRequest.UserData> userDataList) {
        Map<String, UserUpdateRequest.UserData> validData = new HashMap<>();
        
        for (UserUpdateRequest.UserData userData : userDataList) {
            if (userData.getUsername() == null || userData.getUsername().trim().isEmpty()) {
                log.warn("Skipping user with null or empty username");
                continue;
            }
            validData.put(userData.getUsername(), userData);
        }
        
        return validData;
    }

    private void updateUserWithData(User user, UserUpdateRequest.UserData userData) {
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