package com.user.demo.dto;

/**
 * DTO for individual user data in bulk user operations.
 */
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UserRequest {
    @JsonProperty("employee_id")
    private String employeeId;
    
    private String fullname;
    private String department;
    private String position;
    private String email;
    
    @JsonProperty("phone_number")
    private String phoneNumber;
    
    @JsonProperty("birth_year")
    private String birthYear;
} 