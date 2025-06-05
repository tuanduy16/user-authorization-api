package com.user.demo.dto;

/**
 * DTO for bulk user requests, containing a list of users and a delete flag.
 */
import lombok.Data;
import java.util.List;

@Data
public class UserBulkRequest {
    private List<UserRequest> data;
    private boolean deleteNonExistPeople;
} 