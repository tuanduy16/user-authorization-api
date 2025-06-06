package com.user.demo.controller;

/**
 * Handles user-related API endpoints, including bulk user operations and permission updates.
 */
import com.user.demo.dto.UserBulkRequest;
import com.user.demo.dto.UserUpdateRequest;
import com.user.demo.service.UserService;
import com.user.demo.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @PostMapping("/bulk")
    public ResponseEntity<?> upsertUsers(@RequestBody UserBulkRequest request) {
        try {
            log.info("Received bulk request: {}", request);
            userService.upsertUsers(request);
            return ResponseEntity.ok("Users processed successfully");
        } catch (BusinessException e) {
            log.error("Business error in bulk request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in bulk request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An unexpected error occurred: " + e.getMessage());
        }
    }

    @PostMapping("/update")
    public ResponseEntity<String> updateUsers(@RequestBody UserUpdateRequest request) {
        userService.updateUsers(request);
        return ResponseEntity.ok("User permissions updated successfully");
    }
} 