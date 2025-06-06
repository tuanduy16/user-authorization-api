package com.user.demo.dto;

/**
 * DTO for user permission update requests, including location and permission data.
 */
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class UserUpdateRequest {
    @NotEmpty(message = "Data list cannot be empty")
    private List<UserData> data;

    @Data
    public static class UserData {
        @NotBlank(message = "Username is required")
        private String username;

        @JsonProperty("is_allowed")
        private boolean isAllowed;

        @NotEmpty(message = "Agent list cannot be empty")
        private List<String> agent;

        @NotEmpty(message = "Field list cannot be empty")
        private List<String> field;

        @NotNull(message = "Location permission is required")
        @JsonProperty("location_permission")
        private LocationPermission locationPermission;
    }

    @Data
    public static class LocationPermission {
        @NotBlank(message = "Level is required")
        @JsonProperty("level")
        private String level;  // "nation", "area", "province", "district", "main_station", "station"

        @NotBlank(message = "Value is required")
        @JsonProperty("value")
        private String value;
    }
}