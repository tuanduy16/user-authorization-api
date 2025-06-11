package com.user.demo.service;

import com.user.demo.dto.LocationDTO;
import com.user.demo.model.*;
import com.user.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class LocationService {
    
    @Autowired
    private NationRepository nationRepository;
    
    @Autowired
    private AreaRepository areaRepository;
    
    @Autowired
    private ProvinceRepository provinceRepository;
    
    @Autowired
    private DistrictRepository districtRepository;
    
    @Autowired
    private MainStationRepository mainStationRepository;

    @Transactional(readOnly = true)
    public LocationDTO getAllLocations() {
        LocationDTO locationDTO = new LocationDTO();
        
        // Fetch all data in parallel using CompletableFuture
        CompletableFuture<List<Nation>> nationsFuture = CompletableFuture.supplyAsync(() -> nationRepository.findAll());
        CompletableFuture<List<Area>> areasFuture = CompletableFuture.supplyAsync(() -> areaRepository.findAll());
        CompletableFuture<List<Province>> provincesFuture = CompletableFuture.supplyAsync(() -> provinceRepository.findAll());
        CompletableFuture<List<District>> districtsFuture = CompletableFuture.supplyAsync(() -> districtRepository.findAll());
        CompletableFuture<List<MainStation>> mainStationsFuture = CompletableFuture.supplyAsync(() -> mainStationRepository.findAll());
        
        // Wait for all futures to complete
        CompletableFuture.allOf(nationsFuture, areasFuture, provincesFuture, districtsFuture, mainStationsFuture).join();
        
        // Convert to DTOs in parallel streams
        locationDTO.setNations(nationsFuture.join().parallelStream()
            .map(nation -> {
                LocationDTO.NationDTO dto = new LocationDTO.NationDTO();
                dto.setCode(nation.getCode());
                dto.setName(nation.getName());
                return dto;
            })
            .collect(Collectors.toList()));
            
        locationDTO.setAreas(areasFuture.join().parallelStream()
            .map(area -> {
                LocationDTO.AreaDTO dto = new LocationDTO.AreaDTO();
                dto.setCode(area.getCode());
                dto.setName(area.getName());
                return dto;
            })
            .collect(Collectors.toList()));
            
        locationDTO.setProvinces(provincesFuture.join().parallelStream()
            .map(province -> {
                LocationDTO.ProvinceDTO dto = new LocationDTO.ProvinceDTO();
                dto.setCode(province.getCode());
                dto.setName(province.getName());
                dto.setType(province.getType());
                dto.setAreaCode(province.getArea().getCode());
                return dto;
            })
            .collect(Collectors.toList()));
            
        locationDTO.setDistricts(districtsFuture.join().parallelStream()
            .map(district -> {
                LocationDTO.DistrictDTO dto = new LocationDTO.DistrictDTO();
                dto.setCode(district.getCode());
                dto.setName(district.getName());
                dto.setType(district.getType());
                dto.setProvinceCode(district.getProvince().getCode());
                return dto;
            })
            .collect(Collectors.toList()));
            
        locationDTO.setMainStations(mainStationsFuture.join().parallelStream()
            .map(station -> {
                LocationDTO.MainStationDTO dto = new LocationDTO.MainStationDTO();
                dto.setCode(station.getCode());
                dto.setName(station.getName());
                return dto;
            })
            .collect(Collectors.toList()));
        
        return locationDTO;
    }
} 