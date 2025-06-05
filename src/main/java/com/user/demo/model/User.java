package com.user.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
// Represents a user in the system
public class User {
    @Id
    private String username; // Username (primary key)
    private String email; // User email
    private String employeeId; // Employee ID
    private String fullName; // Full name
    private String phoneNumber; // Phone number
    private String birthYear; // Birth year
    private String position; // Position
    private String department; // Department
    private String agentPermission; // Agent permissions (comma-separated)
    private String fieldPermission; // Field permissions (comma-separated)
    private Boolean isAllowed; // Whether the user is allowed
    private LocalDateTime approvedAt; // Approval timestamp
} 