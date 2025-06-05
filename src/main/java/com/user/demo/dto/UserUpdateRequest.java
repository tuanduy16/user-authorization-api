package com.user.demo.dto;

/**
 * DTO for user permission update requests, including location and permission data.
 */
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class UserUpdateRequest {
    private List<UserData> data;

    @Data
    public static class UserData {
        private String username;

        @JsonProperty("is_allowed")
        private boolean isAllowed;

        private List<String> agent;
        private List<String> field;

        @JsonProperty("location_permission")
        private LocationPermission locationPermission;
    }

    @Data
    public static class LocationPermission {
        @JsonProperty("level")
        private String level;  // "nation", "area", "province", "district", "main_station", "station"

        @JsonProperty("value")
        private String value;
    }
}