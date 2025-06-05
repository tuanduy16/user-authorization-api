package com.user.demo.controller;

/**
 * Handles station-related API endpoints, including syncing station data.
 */
import com.user.demo.service.StationSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stations")
public class StationController {
    @Autowired
    private StationSyncService stationSyncService;

    @PostMapping("/sync")
    public ResponseEntity<String> syncStations() {
        stationSyncService.syncStations();
        return ResponseEntity.ok("Station sync triggered successfully");
    }
} 