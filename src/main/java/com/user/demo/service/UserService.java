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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Optional;

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
    public void upsertUsers(UserBulkRequest request) {
        List<UserRequest> userRequests = request.getData();
        Set<String> usernamesInRequest = new HashSet<>();

        for (UserRequest userReq : userRequests) {
            Optional<String> usernameOpt = extractUsernameFromEmail(userReq.getEmail());
            if (usernameOpt.isEmpty()) continue; // skip if email is null
            String username = usernameOpt.get();
            usernamesInRequest.add(username);
            User user = userRepository.findById(username).orElseGet(User::new);
            user.setUsername(username);
            user.setEmail(userReq.getEmail());
            user.setEmployeeId(userReq.getEmployeeId());
            user.setFullName(userReq.getFullname());
            user.setPhoneNumber(userReq.getPhoneNumber());
            user.setBirthYear(userReq.getBirthYear());
            user.setPosition(userReq.getPosition());
            user.setDepartment(userReq.getDepartment());
            if (user.getAgentPermission() == null) user.setAgentPermission("");
            if (user.getFieldPermission() == null) user.setFieldPermission("");
            if (user.getIsAllowed() == null) user.setIsAllowed(false);
            userRepository.save(user);

            // Create location permission entry if it doesn't exist
            locationPermissionRepository.findById(username).orElseGet(() -> {
                LocationPermission lp = new LocationPermission();
                lp.setUsername(username);
                return locationPermissionRepository.save(lp);
            });
        }

        if (request.isDeleteNonExistPeople()) {
            List<User> allUsers = userRepository.findAll();
            for (User user : allUsers) {
                if (!usernamesInRequest.contains(user.getUsername())) {
                    userRepository.delete(user);
                    locationPermissionRepository.deleteById(user.getUsername());
                }
            }
        }
    }

    /**
     * Extract username from email address.
     */
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

        for (UserUpdateRequest.UserData userData : request.getData()) {
            log.info("Processing user: {}", userData.getUsername());
            validateUserData(userData);

            User user = userRepository.findById(userData.getUsername())
                    .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User " + userData.getUsername() + " not found"));

            if (!userData.isAllowed()) {
                log.info("Taking away permission for user: {}", userData.getUsername());
                user.setIsAllowed(false);
                user.setAgentPermission("");
                user.setFieldPermission("");
                userRepository.save(user);
                log.info("User {} disabled and saved", userData.getUsername());
                locationPermissionRepository.deleteById(userData.getUsername());
                log.info("LocationPermission deleted for user {}", userData.getUsername());
            } else {
                log.info("Granting permission for user: {}", userData.getUsername());
                user.setIsAllowed(true);
                user.setAgentPermission(userData.getAgent() == null || userData.getAgent().isEmpty() ? "" : String.join(",", userData.getAgent()));
                user.setFieldPermission(userData.getField() == null || userData.getField().isEmpty() ? "" : String.join(",", userData.getField()));
                user.setApprovedAt(LocalDateTime.now());
                userRepository.save(user);
                log.info("User {} approved and saved", userData.getUsername());

                // Update existing location_permission
                LocationPermission locationPermission = locationPermissionRepository.findById(userData.getUsername())
                        .orElseThrow(() -> new BusinessException("LOCATION_PERMISSION_NOT_FOUND", 
                            "Location permission not found for user " + userData.getUsername()));
                
                String level = userData.getLocationPermission().getLevel();
                String value = userData.getLocationPermission().getValue();
                switch (level) {
                    case "nation" -> locationPermission.setNation(value);
                    case "area" -> locationPermission.setArea(value);
                    case "province" -> locationPermission.setProvince(value);
                    case "district" -> locationPermission.setDistrict(value);
                    case "main_station" -> locationPermission.setMainStation(value);
                    case "station" -> locationPermission.setStation(value);
                    default -> throw new BusinessException("INVALID_LOCATION_LEVEL", "Invalid location level: " + level);
                }
                locationPermissionRepository.save(locationPermission);
                log.info("LocationPermission updated for user {}", userData.getUsername());
            }
        }
    }

    /**
     * Validate user update data (agents, fields, location codes).
     */
    private void validateUserData(UserUpdateRequest.UserData userData) {
        if (userData.getUsername() == null || userData.getUsername().trim().isEmpty()) {
            throw new BusinessException("INVALID_USERNAME", "Username cannot be empty");
        }

        if (!userData.isAllowed()) {
            return; // No further validation needed for disabled users
        }

        // Validate agent IDs
        for (String agentId : Optional.ofNullable(userData.getAgent()).orElse(List.of())) {
            if (agentRepository.findById(Long.parseLong(agentId)).isEmpty()) {
                throw new BusinessException("INVALID_AGENT", "Agent ID " + agentId + " does not exist");
            }
        }

        // Validate field IDs
        for (String fieldId : Optional.ofNullable(userData.getField()).orElse(List.of())) {
            if (fieldRepository.findById(Long.parseLong(fieldId)).isEmpty()) {
                throw new BusinessException("INVALID_FIELD", "Field ID " + fieldId + " does not exist");
            }
        }

        // Validate location permission
        if (userData.getLocationPermission() != null) {
            String level = userData.getLocationPermission().getLevel();
            if (level == null || !isValidLocationLevel(level)) {
                throw new BusinessException("INVALID_LOCATION_LEVEL", 
                    "Location level must be one of: nation, area, province, district, main_station, station");
            }
            String value = userData.getLocationPermission().getValue();
            if (value == null) {
                throw new BusinessException("INVALID_LOCATION_VALUE", "Location value cannot be null");
            }
            // Split value by comma and validate each code
            String[] codes = value.split(",");
            for (String code : codes) {
                code = code.trim();
                boolean exists = switch (level) {
                    case "nation" -> nationRepository.existsById(code);
                    case "area" -> areaRepository.existsById(code);
                    case "province" -> provinceRepository.existsById(code);
                    case "district" -> districtRepository.existsById(code);
                    case "main_station" -> mainStationRepository.existsById(code);
                    case "station" -> stationRepository.existsById(code);
                    default -> false;
                };
                if (!exists) {
                    throw new BusinessException("INVALID_LOCATION_CODE", "Code '" + code + "' does not exist for level '" + level + "'");
                }
            }
        }
    }

    /**
     * Check if the location level is valid.
     */
    private boolean isValidLocationLevel(String level) {
        return Set.of("nation", "area", "province", "district", "main_station", "station")
                .contains(level);
    }
} 