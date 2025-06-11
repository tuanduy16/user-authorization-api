package com.user.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UserResponseDTO {
    private String username;
    private String email;
    
    @JsonProperty("employee_id")
    private String employeeId;
    
    private String fullname;
    private String department;
    private String position;
    
    @JsonProperty("phone_number")
    private String phoneNumber;
    
    @JsonProperty("birth_year")
    private String birthYear;
    
    @JsonProperty("is_allowed")
    private Boolean isAllowed;
    
    @JsonProperty("agent_permission")
    private String agentPermission;
    
    @JsonProperty("field_permission")
    private String fieldPermission;
    
    @JsonProperty("location_permission")
    private LocationPermissionDTO locationPermission;
    
    @Data
    public static class LocationPermissionDTO {
        private String nation;
        private String area;
        private String province;
        private String district;
        
        @JsonProperty("main_station")
        private String mainStation;
        
        private String station;
        
        @JsonProperty("station_default")
        private String stationDefault;
    }
}