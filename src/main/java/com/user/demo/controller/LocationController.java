package com.user.demo.controller;

import com.user.demo.dto.LocationDTO;
import com.user.demo.service.LocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/locations")
public class LocationController {
    private static final Logger log = LoggerFactory.getLogger(LocationController.class);

    @Autowired
    private LocationService locationService;

    @GetMapping
    public ResponseEntity<LocationDTO> getAllLocations() {
        log.info("Fetching all location data");
        LocationDTO locations = locationService.getAllLocations();
        return ResponseEntity.ok(locations);
    }
} 