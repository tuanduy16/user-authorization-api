package com.user.demo.dto;

import lombok.Data;
import java.util.List;

@Data
public class LocationDTO {
    private List<NationDTO> nations;
    private List<AreaDTO> areas;
    private List<ProvinceDTO> provinces;
    private List<DistrictDTO> districts;
    private List<MainStationDTO> mainStations;

    @Data
    public static class NationDTO {
        private String code;
        private String name;
    }

    @Data
    public static class AreaDTO {
        private String code;
        private String name;
    }

    @Data
    public static class ProvinceDTO {
        private String code;
        private String name;
        private String type;
        private String areaCode;
    }

    @Data
    public static class DistrictDTO {
        private String code;
        private String name;
        private String type;
        private String provinceCode;
    }

    @Data
    public static class MainStationDTO {
        private String code;
        private String name;
    }
} 