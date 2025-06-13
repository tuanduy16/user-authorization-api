package com.user.demo.service;

/**
 * Service for user management, including bulk operations, permission updates, and validation.
 */
import com.user.demo.dto.UserResponseDTO;
import com.user.demo.dto.UserBulkRequest;
import com.user.demo.model.Agent;
import com.user.demo.model.Field;
import com.user.demo.dto.UserRequest;
import com.user.demo.dto.UserUpdateRequest;
import com.user.demo.exception.BusinessException;
import com.user.demo.model.User;
import com.user.demo.model.Nation;
import com.user.demo.model.Area;
import com.user.demo.model.Province;
import com.user.demo.model.District;
import com.user.demo.model.MainStation;
import com.user.demo.model.Station;
import com.user.demo.repository.UserRepository;
import com.user.demo.repository.AgentRepository;
import com.user.demo.repository.FieldRepository;
import com.user.demo.repository.NationRepository;
import com.user.demo.repository.AreaRepository;
import com.user.demo.repository.ProvinceRepository;
import com.user.demo.repository.DistrictRepository;
import com.user.demo.repository.MainStationRepository;
import com.user.demo.repository.StationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
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

    private static final Set<String> validLocationLevels = new HashSet<>(Arrays.asList(
        "nation", "area", "province", "district", "main_station", "station"
    ));

    /**
     * Bulk create or update users, and optionally delete non-existent users.
     */
    @Transactional
    @CacheEvict(value = {"users", "permissions"}, allEntries = true)
    public int insertUsers(UserBulkRequest request) {
        log.info("insertUsers called with data: {}", request.getData());
        
        // Validate request
        if (request.getData() == null || request.getData().isEmpty()) {
            log.warn("Request data is null or empty");
            throw new BusinessException("INVALID_REQUEST", "Request data cannot be empty");
        }

        // Extract and validate all usernames first
        Map<String, UserRequest> validRequests = extractValidUserRequests(request.getData());
        if (validRequests.isEmpty()) {
            log.warn("No valid user data found");
            return 0;
        }

        // Get existing users to handle updates
        Map<String, User> existingUsers = userRepository.findAllById(validRequests.keySet())
            .stream()
            .collect(Collectors.toMap(User::getUsername, user -> user));

        // Create and save users
        List<User> users = new ArrayList<>();
        for (Map.Entry<String, UserRequest> entry : validRequests.entrySet()) {
            String username = entry.getKey();
            UserRequest userReq = entry.getValue();

            User user;
            if (existingUsers.containsKey(username)) {
                // Update existing user while preserving permissions
                user = existingUsers.get(username);
                user.setEmail(userReq.getEmail());
                user.setEmployeeId(userReq.getEmployeeId());
                user.setFullname(userReq.getFullname());
                user.setPhoneNumber(userReq.getPhoneNumber());
                user.setBirthYear(userReq.getBirthYear());
                user.setPosition(userReq.getPosition());
                user.setDepartment(userReq.getDepartment());
            } else {
                // Create new user
                user = new User();
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
                user.setLocationPermissionLevel(null);
                user.setLocationPermissionValue(null);
                user.setApprovedAt(null);
            }
            users.add(user);
        }

        userRepository.saveAll(users);
        
        // Handle deletions if requested
        if (request.isDeleteNonExistPeople()) {
            handleUserDeletions(validRequests.keySet());
        }
        
        log.info("Successfully processed {} users", users.size());
        return users.size();
    }

    @Transactional
    @CacheEvict(value = {"users", "permissions"}, allEntries = true)
    public int updateUsers(UserUpdateRequest request) {
        log.info("updateUsers called with data: {}", request.getData());
        
        // Validate request
        if (request.getData() == null || request.getData().isEmpty()) {
            log.warn("Request data is null or empty");
            throw new BusinessException("INVALID_REQUEST", "Request data cannot be empty");
        }

        // Extract and validate all usernames first
        Map<String, UserUpdateRequest.UserData> validUserData = extractValidUserData(request.getData());
        if (validUserData.isEmpty()) {
            log.warn("No valid user data found");
            return 0;
        }

        log.info("Found {} valid user records to process", validUserData.size());

        // Pre-fetch all users
        Map<String, User> existingUsers = userRepository.findAllById(validUserData.keySet())
            .stream()
            .collect(Collectors.toMap(User::getUsername, user -> user));

        log.info("Found {} existing users", existingUsers.size());

        // Validate all users exist
        for (String username : validUserData.keySet()) {
            if (!existingUsers.containsKey(username)) {
                throw new BusinessException("USER_NOT_FOUND", "User " + username + " not found");
            }
        }

        // Collect all codes for validation
        Set<String> agentCodes = new HashSet<>();
        Set<String> fieldCodes = new HashSet<>();
        Map<String, Set<String>> locationValuesByLevel = new HashMap<>();

        // First pass: collect all values for validation
        for (UserUpdateRequest.UserData userData : validUserData.values()) {
            if (userData.isAllowed()) {
                // Collect agent codes
                if (userData.getAgent() != null) {
                    agentCodes.addAll(userData.getAgent());
                }
                
                // Collect field codes
                if (userData.getField() != null) {
                    fieldCodes.addAll(userData.getField());
                }
                
                // Collect location values
                if (userData.getLocationPermission() != null) {
                    String level = userData.getLocationPermission().getLevel().toLowerCase();
                    String value = userData.getLocationPermission().getValue();
                    
                    if (value != null && !value.trim().isEmpty()) {
                        Set<String> values = Arrays.stream(value.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toSet());
                        
                        locationValuesByLevel.computeIfAbsent(level, k -> new HashSet<>()).addAll(values);
                    }
                }
            }
        }

        log.info("Collected {} agent codes, {} field codes, and {} location levels for validation", 
            agentCodes.size(), fieldCodes.size(), locationValuesByLevel.size());

        // Validate agent codes in batch
        if (!agentCodes.isEmpty()) {
            List<Long> agentIds = agentCodes.stream()
                .map(Long::parseLong)
                .collect(Collectors.toList());
            Set<Long> validAgentIds = agentRepository.findAllById(agentIds).stream()
                .map(Agent::getId)
                .collect(Collectors.toSet());
            
            Set<String> invalidAgentCodes = agentCodes.stream()
                .filter(code -> !validAgentIds.contains(Long.parseLong(code)))
                .collect(Collectors.toSet());
            
            if (!invalidAgentCodes.isEmpty()) {
                throw new BusinessException("INVALID_AGENT", 
                    "Agent codes do not exist: " + String.join(", ", invalidAgentCodes));
            }
            log.info("Successfully validated {} agent codes", agentCodes.size());
        }

        // Validate field codes in batch
        if (!fieldCodes.isEmpty()) {
            List<Long> fieldIds = fieldCodes.stream()
                .map(Long::parseLong)
                .collect(Collectors.toList());
            Set<Long> validFieldIds = fieldRepository.findAllById(fieldIds).stream()
                .map(Field::getId)
                .collect(Collectors.toSet());
            
            Set<String> invalidFieldCodes = fieldCodes.stream()
                .filter(code -> !validFieldIds.contains(Long.parseLong(code)))
                .collect(Collectors.toSet());
            
            if (!invalidFieldCodes.isEmpty()) {
                throw new BusinessException("INVALID_FIELD", 
                    "Field codes do not exist: " + String.join(", ", invalidFieldCodes));
            }
            log.info("Successfully validated {} field codes", fieldCodes.size());
        }

        // Validate location permissions
        for (Map.Entry<String, Set<String>> entry : locationValuesByLevel.entrySet()) {
            String level = entry.getKey();
            Set<String> values = entry.getValue();
            
            log.info("Validating {} values for level: {}", values.size(), level);
            
            // Validate level
            if (!validLocationLevels.contains(level)) {
                throw new BusinessException("INVALID_LEVEL", 
                    "Invalid location level: " + level + ". Must be one of: " + validLocationLevels);
            }
            
            // Validate values exist in database
            switch (level) {
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
                default:
                    throw new BusinessException("INVALID_LEVEL", 
                        "Invalid location level: " + level + ". Must be one of: " + validLocationLevels);
            }
        }

        // Process updates in memory
        List<User> usersToUpdate = new ArrayList<>();
        for (Map.Entry<String, UserUpdateRequest.UserData> entry : validUserData.entrySet()) {
            String username = entry.getKey();
            UserUpdateRequest.UserData userData = entry.getValue();
            
            log.info("Processing user: {}", username);
            User user = existingUsers.get(username);
            
            // Update basic fields
            user.setIsAllowed(userData.isAllowed());
            
            if (userData.isAllowed()) {
                // Set agent permissions
                if (userData.getAgent() != null && !userData.getAgent().isEmpty()) {
                    user.setAgentPermission(String.join(",", userData.getAgent()));
                } else {
                    user.setAgentPermission("");
                }

                // Set field permissions
                if (userData.getField() != null && !userData.getField().isEmpty()) {
                    user.setFieldPermission(String.join(",", userData.getField()));
                } else {
                    user.setFieldPermission("");
                }

                user.setApprovedAt(LocalDateTime.now());
                
                // Update location permission
                if (userData.getLocationPermission() != null) {
                    String level = userData.getLocationPermission().getLevel().toLowerCase();
                    String value = userData.getLocationPermission().getValue();
                    
                    if (value != null && !value.trim().isEmpty()) {
                        user.setLocationPermissionLevel(level);
                        user.setLocationPermissionValue(value.trim());
                        log.info("Setting location permission for user {}: level={}, value={}", 
                            username, level, value.trim());
                    } else {
                        user.setLocationPermissionLevel(null);
                        user.setLocationPermissionValue(null);
                        log.info("Clearing location permission for user {} due to empty value", username);
                    }
                } else {
                    user.setLocationPermissionLevel(null);
                    user.setLocationPermissionValue(null);
                    log.info("Clearing location permission for user {} due to null permission", username);
                }
            } else {
                // Clear permissions if user is not allowed
                user.setAgentPermission("");
                user.setFieldPermission("");
                user.setLocationPermissionLevel(null);
                user.setLocationPermissionValue(null);
                user.setApprovedAt(null);
                log.info("Clearing all permissions for user {} due to not allowed", username);
            }
            
            usersToUpdate.add(user);
        }

        // Save all updates in one batch
        if (!usersToUpdate.isEmpty()) {
            userRepository.saveAll(usersToUpdate);
            log.info("Successfully updated {} users in batch", usersToUpdate.size());
        }

        return usersToUpdate.size();
    }

    private <T> void validateLocationValues(JpaRepository<T, String> repository, Set<String> values, String entityName) {
        // Fetch all values in one batch
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
        
        // Find all invalid values
        Set<String> invalidValues = values.stream()
            .filter(value -> !existingIds.contains(value))
            .collect(Collectors.toSet());
        
        if (!invalidValues.isEmpty()) {
            throw new BusinessException("INVALID_" + entityName.toUpperCase(), 
                entityName + " codes do not exist: " + String.join(", ", invalidValues));
        }

        log.info("Successfully validated {} {} codes", values.size(), entityName.toLowerCase());
    }

    private Map<String, UserRequest> extractValidUserRequests(List<UserRequest> requests) {
        Map<String, UserRequest> validRequests = new HashMap<>();

        for (UserRequest request : requests) {
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                log.warn("Skipping request with empty email");
                continue;
            }

            String username = request.getEmail().split("@")[0].trim();
            validRequests.put(username, request);
        }

        return validRequests;
    }

    private Map<String, UserUpdateRequest.UserData> extractValidUserData(List<UserUpdateRequest.UserData> data) {
        Map<String, UserUpdateRequest.UserData> validData = new HashMap<>();
        Set<String> seenUsernames = new HashSet<>();

        for (UserUpdateRequest.UserData userData : data) {
            if (userData.getUsername() == null || userData.getUsername().trim().isEmpty()) {
                log.warn("Skipping data with empty username");
                continue;
            }

            String username = userData.getUsername().trim();
            if (seenUsernames.contains(username)) {
                log.warn("Skipping duplicate username: {}", username);
                continue;
            }

            seenUsernames.add(username);
            validData.put(username, userData);
        }

        return validData;
    }

    public Page<UserResponseDTO> getUsers(Boolean isAllowed, String username, String department, Pageable pageable) {
        Specification<User> spec = Specification.where(null);

        if (isAllowed != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isAllowed"), isAllowed));
        }

        if (username != null && !username.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("username")), 
                "%" + username.toLowerCase().trim() + "%"));
        }

        if (department != null && !department.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("department")), 
                "%" + department.toLowerCase().trim() + "%"));
        }

        Page<User> users = userRepository.findAll(spec, pageable);
        return users.map(user -> {
            UserResponseDTO dto = new UserResponseDTO();
            dto.setUsername(user.getUsername());
            dto.setEmail(user.getEmail());
            dto.setEmployeeId(user.getEmployeeId());
            dto.setFullname(user.getFullname());
            dto.setDepartment(user.getDepartment());
            dto.setPosition(user.getPosition());
            dto.setPhoneNumber(user.getPhoneNumber());
            dto.setBirthYear(user.getBirthYear());
            dto.setIsAllowed(user.getIsAllowed());
            dto.setAgentPermission(user.getAgentPermission());
            dto.setFieldPermission(user.getFieldPermission());
            dto.setLocationPermissionLevel(user.getLocationPermissionLevel());
            dto.setLocationPermissionValue(user.getLocationPermissionValue());
            dto.setStationDefault(user.getStationDefault());
            return dto;
        });
    }

    private void handleUserDeletions(Set<String> validUsernames) {
        log.info("Handling deletion of non-existent users");
        try {
            List<User> usersToDelete = userRepository.findAll().stream()
                .filter(user -> !validUsernames.contains(user.getUsername()))
                .collect(Collectors.toList());

            if (!usersToDelete.isEmpty()) {
                userRepository.deleteAll(usersToDelete);
                log.info("Deleted {} users in batch", usersToDelete.size());
            }
        } catch (Exception e) {
            log.error("Error deleting non-existent users: {}", e.getMessage(), e);
            throw new BusinessException("DELETE_ERROR", "Failed to delete non-existent users: " + e.getMessage());
        }
    }

    public void bulkUpdateUsers(UserBulkRequest request) {
        if (request.getData() == null || request.getData().isEmpty()) {
            log.warn("Empty user data received");
            return;
        }

        // Extract and validate all usernames first
        Map<String, UserRequest> validRequests = extractValidUserRequests(request.getData());
        if (validRequests.isEmpty()) {
            log.warn("No valid user data found");
            return;
        }

        // Handle deletions if requested
        if (request.isDeleteNonExistPeople()) {
            handleUserDeletions(validRequests.keySet());
        }

        // Process updates and new users
        List<User> usersToSave = new ArrayList<>();
        
        // First, get all existing users to preserve their permissions
        Map<String, User> existingUsers = userRepository.findAllById(validRequests.keySet())
            .stream()
            .collect(Collectors.toMap(User::getUsername, user -> user));

        for (Map.Entry<String, UserRequest> entry : validRequests.entrySet()) {
            String username = entry.getKey();
            UserRequest userReq = entry.getValue();

            User user;
            if (existingUsers.containsKey(username)) {
                // Update existing user while preserving permissions
                user = existingUsers.get(username);
                user.setEmail(userReq.getEmail());
                user.setEmployeeId(userReq.getEmployeeId());
                user.setFullname(userReq.getFullname());
                user.setPhoneNumber(userReq.getPhoneNumber());
                user.setBirthYear(userReq.getBirthYear());
                user.setPosition(userReq.getPosition());
                user.setDepartment(userReq.getDepartment());
            } else {
                // Create new user
                user = new User();
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
                user.setLocationPermissionLevel(null);
                user.setLocationPermissionValue(null);
                user.setApprovedAt(null);
            }
            usersToSave.add(user);
        }

        try {
            userRepository.saveAll(usersToSave);
            log.info("Successfully processed {} users", usersToSave.size());
        } catch (Exception e) {
            log.error("Error saving users: {}", e.getMessage());
            throw new RuntimeException("Failed to save users: " + e.getMessage());
        }
    }
} 