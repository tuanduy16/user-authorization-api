package com.user.demo.dto;

/**
 * DTO for individual user data in bulk user operations.
 */
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;

@Data
public class UserRequest {
    @NotBlank(message = "Employee ID is required")
    @JsonProperty("employee_id")
    private String employeeId;
    
    @NotBlank(message = "Full name is required")
    private String fullname;
    
    @NotBlank(message = "Department is required")
    private String department;
    
    @NotBlank(message = "Position is required")
    private String position;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\d{10,11}$", message = "Phone number must be 10-11 digits")
    @JsonProperty("phone_number")
    private String phoneNumber;
    
    @NotBlank(message = "Birth year is required")
    @Pattern(regexp = "^\\d{4}$", message = "Birth year must be 4 digits")
    @JsonProperty("birth_year")
    private String birthYear;
} 