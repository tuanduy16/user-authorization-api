package com.user.demo.controller;

/**
 * Handles user-related API endpoints, including bulk user operations and permission updates.
 */
import com.user.demo.dto.UserBulkRequest;
import com.user.demo.dto.UserUpdateRequest;
import com.user.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/bulk")
    public ResponseEntity<String> upsertUsers(@RequestBody UserBulkRequest request) {
        userService.upsertUsers(request);
        return ResponseEntity.ok("Users processed successfully");
    }

    @PostMapping("/update")
    public ResponseEntity<String> updateUsers(@RequestBody UserUpdateRequest request) {
        userService.updateUsers(request);
        return ResponseEntity.ok("User permissions updated successfully");
    }
} 