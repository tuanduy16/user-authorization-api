package com.user.demo.dto;

/**
 * DTO for bulk user requests, containing a list of users and a delete flag.
 */
import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class UserBulkRequest {
    @Valid
    @NotEmpty(message = "Data list cannot be empty")
    private List<UserRequest> data;
    
    @NotNull(message = "Delete non exist people flag is required")
    private boolean deleteNonExistPeople;
} 