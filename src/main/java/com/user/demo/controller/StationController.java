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
import com.user.demo.dto.StationDTO;
import com.user.demo.service.StationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.user.demo.model.Station;
import com.user.demo.repository.StationRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/stations")
public class StationController {
    @Autowired
    private StationSyncService stationSyncService;

    @Autowired
    private StationService stationService;

    @Autowired
    private StationRepository stationRepository;

    @PostMapping("/sync")
    public ResponseEntity<String> syncStations() {
        stationSyncService.syncStations();
        return ResponseEntity.ok("Station sync triggered successfully");
    }

    @GetMapping
    public ResponseEntity<?> getStations(@RequestParam(required = false) String search) {
        if (search != null && !search.trim().isEmpty()) {
            log.info("Searching for station with code: {}", search);
            StationDTO station = stationService.findByCode(search.trim());
            
            if (station == null) {
                log.info("No station found with code: {}", search);
                return ResponseEntity.ok("Station not found");
            }

            log.info("Found station: {}", station);
            return ResponseEntity.ok(station);
        }

        // If no search parameter, return all stations
        log.info("Getting all stations");
        List<Station> stations = stationRepository.findAll();
        log.info("Found {} stations", stations.size());

        List<StationDTO> stationDTOs = stations.stream()
            .map(station -> {
                StationDTO dto = new StationDTO();
                dto.setCode(station.getCode());
                return dto;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(stationDTOs);
    }
} 