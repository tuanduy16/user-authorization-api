package com.user.demo.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "location_permissions")
public class LocationPermission {
    @Id
    private String username; // Username (primary key, links to User)
    private String nation; // Nation code
    private String area; // Area code
    private String province; // Province code
    private String district; // District code
    private String mainStation; // Main station code
    private String station; // Station code
    
    /**
     * Default station for the user.
     * Can only be updated through the station update API.
     */
    @Column(name = "station_default", nullable = true)
    private String stationDefault; // Default station code
} 