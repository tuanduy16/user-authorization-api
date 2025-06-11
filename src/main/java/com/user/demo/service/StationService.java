package com.user.demo.service;

import com.user.demo.dto.StationDTO;
import com.user.demo.model.Station;
import com.user.demo.repository.StationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class StationService {
    
    @Autowired
    private StationRepository stationRepository;

    @Transactional(readOnly = true)
    public StationDTO findByCode(String code) {
        log.info("Finding station with code: {}", code);
        Station station = stationRepository.findById(code).orElse(null);
        
        if (station == null) {
            log.info("No station found with code: {}", code);
            return null;
        }

        StationDTO dto = new StationDTO();
        dto.setCode(station.getCode());
        
        log.info("Found station: {}", dto);
        return dto;
    }
} 