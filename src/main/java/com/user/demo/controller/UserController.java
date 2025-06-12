package com.user.demo.controller;

/**
 * Handles user-related API endpoints, including bulk user operations and permission updates.
 */
import com.user.demo.dto.UserBulkRequest;
import com.user.demo.dto.UserUpdateRequest;
import com.user.demo.dto.UserResponseDTO;
import com.user.demo.service.UserService;
import com.user.demo.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<Page<UserResponseDTO>> getUsers(
            @RequestParam(value = "is_allowed", required = false) Boolean isAllowed,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("Received GET request with filters - is_allowed: {}, username: {}, department: {}", 
            isAllowed, username, department);

        // Validate pagination parameters
        if (page < 0) {
            throw new BusinessException("INVALID_PAGE", "Page number must be non-negative");
        }
        if (size < 1 || size > 100) {
            throw new BusinessException("INVALID_SIZE", "Page size must be between 1 and 100");
        }

        // Create pageable object
        PageRequest pageRequest = PageRequest.of(page, size);

        // Get users with filters
        Page<UserResponseDTO> users = userService.getUsers(isAllowed, username, department, pageRequest);
        
        log.info("Found {} users matching the filters", users.getTotalElements());
        return ResponseEntity.ok(users);
    }

    @PostMapping("/bulk")
    public ResponseEntity<?> insertUsers(@RequestBody UserBulkRequest request) {
        log.info("Received bulk request: {}", request);
        int processedCount = userService.insertUsers(request);
        return ResponseEntity.ok(String.format("Successfully processed %d users", processedCount));
    }

    @PostMapping("/update")
    public ResponseEntity<String> updateUsers(@RequestBody UserUpdateRequest request) {
        int updatedCount = userService.updateUsers(request);
        return ResponseEntity.ok(String.format("Successfully updated permissions for %d users", updatedCount));
    }
} 