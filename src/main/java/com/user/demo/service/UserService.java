package com.user.demo.service;

/**
 * Service for user management, including bulk operations, permission updates, and validation.
 */
import com.user.demo.dto.UserBulkRequest;
import com.user.demo.model.Agent;
import com.user.demo.model.Field;
import com.user.demo.dto.UserRequest;
import com.user.demo.dto.UserUpdateRequest;
import com.user.demo.exception.BusinessException;
import com.user.demo.model.User;
import com.user.demo.model.LocationPermission;
import com.user.demo.model.Nation;
import com.user.demo.model.Area;
import com.user.demo.model.Province;
import com.user.demo.model.District;
import com.user.demo.model.MainStation;
import com.user.demo.model.Station;
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
import org.springframework.data.jpa.repository.JpaRepository;
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
    public void insertUsers(UserBulkRequest request) { 
        log.info("Starting bulk user insert with request: {}", request);
        
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
        
        log.info("Bulk user insert completed successfully");
    }

    private Map<String, UserRequest> extractValidUserRequests(List<UserRequest> requests) {
        Map<String, UserRequest> validRequests = new HashMap<>();
        
        for (UserRequest userReq : requests) {
            if (userReq.getEmail() == null || userReq.getEmail().trim().isEmpty()) {
                log.warn("Skipping user with null or empty email");
                continue;
            }
            
            Optional<String> usernameOpt = extractUsernameFromEmail(userReq.getEmail());
            if (!usernameOpt.isPresent()) {
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

        // Pre-fetch all users
        Map<String, User> existingUsers = userRepository.findAllById(validUserData.keySet())
            .stream()
            .collect(Collectors.toMap(User::getUsername, user -> user));

        // Validate all users exist
        for (String username : validUserData.keySet()) {
            if (!existingUsers.containsKey(username)) {
                throw new BusinessException("USER_NOT_FOUND", "User " + username + " not found");
            }
        }

        // Collect all validation data
        Set<String> agentCodes = new HashSet<>();
        Set<String> fieldCodes = new HashSet<>();
        Map<String, Set<String>> locationValuesByLevel = new HashMap<>();

        for (UserUpdateRequest.UserData userData : validUserData.values()) {
            if (userData.isAllowed()) {
                if (userData.getAgent() != null) {
                    agentCodes.addAll(userData.getAgent());
                }
                if (userData.getField() != null) {
                    fieldCodes.addAll(userData.getField());
                }
                if (userData.getLocationPermission() != null) {
                    String level = userData.getLocationPermission().getLevel();
                    String value = userData.getLocationPermission().getValue();
                    locationValuesByLevel.computeIfAbsent(level, k -> new HashSet<>()).add(value);
                }
            }
        }

        // Batch validate agent codes
        if (!agentCodes.isEmpty()) {
            List<Long> agentIds = agentCodes.stream()
                .map(Long::parseLong)
                .collect(Collectors.toList());
            Set<Long> validAgentIds = agentRepository.findAllById(agentIds).stream()
                .map(Agent::getId)
                .collect(Collectors.toSet());
            
            for (String code : agentCodes) {
                if (!validAgentIds.contains(Long.parseLong(code))) {
                    throw new BusinessException("INVALID_AGENT", "Agent code " + code + " does not exist");
                }
            }
        }

        // Batch validate field codes
        if (!fieldCodes.isEmpty()) {
            List<Long> fieldIds = fieldCodes.stream()
                .map(Long::parseLong)
                .collect(Collectors.toList());
            Set<Long> validFieldIds = fieldRepository.findAllById(fieldIds).stream()
                .map(Field::getId)
                .collect(Collectors.toSet());
            
            for (String code : fieldCodes) {
                if (!validFieldIds.contains(Long.parseLong(code))) {
                    throw new BusinessException("INVALID_FIELD", "Field code " + code + " does not exist");
                }
            }
        }

        // Batch validate location values
        for (Map.Entry<String, Set<String>> entry : locationValuesByLevel.entrySet()) {
            String level = entry.getKey();
            Set<String> values = entry.getValue();
            
            if (!isValidLocationLevel(level)) {
                throw new BusinessException("INVALID_LEVEL", "Invalid location level: " + level);
            }

            switch (level.toLowerCase()) {
                case "nation":
                    validateLocationValues(nationRepository, values, "Nation");
                    break;
                case "area":
                    validateLocationValues(areaRepository, values, "Area");
                    break;
                case "province":
                    validateLocationValues(provinceRepository, values, "Province");
                    break;
                case "district":
                    validateLocationValues(districtRepository, values, "District");
                    break;
                case "main_station":
                    validateLocationValues(mainStationRepository, values, "Main station");
                    break;
                case "station":
                    validateLocationValues(stationRepository, values, "Station");
                    break;
            }
        }

        // Process updates in memory
        List<User> usersToUpdate = new ArrayList<>();
        for (Map.Entry<String, UserUpdateRequest.UserData> entry : validUserData.entrySet()) {
            String username = entry.getKey();
            UserUpdateRequest.UserData userData = entry.getValue();
            
            log.info("Processing user: {}", username);
            User user = existingUsers.get(username);
            updateUserWithData(user, userData);
            usersToUpdate.add(user);
        }

        // Save all updates in one batch
        if (!usersToUpdate.isEmpty()) {
            userRepository.saveAll(usersToUpdate);
            log.info("Updated {} users in batch", usersToUpdate.size());
        }
    }

    private <T> void validateLocationValues(JpaRepository<T, String> repository, Set<String> values, String entityName) {
        List<T> existingEntities = repository.findAllById(values);
        Set<String> existingIds = existingEntities.stream()
            .map(entity -> {
                if (entity instanceof Nation) return ((Nation) entity).getCode();
                if (entity instanceof Area) return ((Area) entity).getCode();
                if (entity instanceof Province) return ((Province) entity).getCode();
                if (entity instanceof District) return ((District) entity).getCode();
                if (entity instanceof MainStation) return ((MainStation) entity).getCode();
                if (entity instanceof Station) return ((Station) entity).getCode();
                return entity.toString();
            })
            .collect(Collectors.toSet());
        
        for (String value : values) {
            if (!existingIds.contains(value)) {
                throw new BusinessException("INVALID_" + entityName.toUpperCase(), 
                    entityName + " code " + value + " does not exist");
            }
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

            // Set all location fields to null first
            locationPermission.setNation(null);
            locationPermission.setArea(null);
            locationPermission.setProvince(null);
            locationPermission.setDistrict(null);
            locationPermission.setMainStation(null);
            locationPermission.setStation(null);

            // Set only the specific location level
            String level = userData.getLocationPermission().getLevel();
            String value = userData.getLocationPermission().getValue();
            
            switch (level.toLowerCase()) {
                case "nation":
                    locationPermission.setNation(value);
                    break;
                case "area":
                    locationPermission.setArea(value);
                    break;
                case "province":
                    locationPermission.setProvince(value);
                    break;
                case "district":
                    locationPermission.setDistrict(value);
                    break;
                case "main_station":
                    locationPermission.setMainStation(value);
                    break;
                case "station":
                    locationPermission.setStation(value);
                    break;
            }
        }
    }

    private boolean isValidLocationLevel(String level) {
        return level != null && Arrays.asList("nation", "area", "province", "district", "main_station", "station")
            .contains(level.toLowerCase());
    }
} 