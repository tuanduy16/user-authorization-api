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
    public ResponseEntity<?> insertUsers(@RequestBody UserBulkRequest request) {
        log.info("Received bulk request: {}", request);
        userService.insertUsers(request);
        return ResponseEntity.ok("Users processed successfully");
    }

    @PostMapping("/update")
    public ResponseEntity<String> updateUsers(@RequestBody UserUpdateRequest request) {
        userService.updateUsers(request);
        return ResponseEntity.ok("User permissions updated successfully");
    }
} 