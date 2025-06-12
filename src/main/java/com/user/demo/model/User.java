package com.user.demo.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "users")
@Data
// Represents a user in the system
public class User {
    @Id
    private String username; // Username (primary key)
    private String email; // User email
    private String employeeId; // Employee ID
    private String fullname; // Full name
    private String phoneNumber; // Phone number
    private String birthYear; // Birth year
    private String position; // Position
    private String department; // Department
    private Boolean isAllowed; // Whether the user is allowed
    private String agentPermission;
    private String fieldPermission;
    private LocalDateTime approvedAt; // Approval timestamp
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private LocationPermission locationPermission;
} 