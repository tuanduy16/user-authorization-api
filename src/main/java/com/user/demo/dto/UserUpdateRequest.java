package com.user.demo.dto;

/**
 * DTO for user permission update requests, including location and permission data.
 */
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class UserUpdateRequest {
    @Valid
    @NotEmpty(message = "Data list cannot be empty")
    private List<UserData> data;

    @Data
    public static class UserData {
        @NotBlank(message = "Username is required")
        private String username;

        @NotNull(message = "Is allowed flag is required")
        @JsonProperty("is_allowed")
        private boolean isAllowed;

        private List<String> agent;
        private List<String> field;

        @Valid
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