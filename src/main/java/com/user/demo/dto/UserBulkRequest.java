package com.user.demo.dto;

/**
 * DTO for bulk user requests, containing a list of users and a delete flag.
 */
import lombok.Data;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class UserBulkRequest {
    @NotEmpty(message = "Data list cannot be empty")
    private List<UserRequest> data;
    private boolean deleteNonExistPeople;
} 