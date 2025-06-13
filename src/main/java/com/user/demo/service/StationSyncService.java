package com.user.demo.service;

/**
 * Service for syncing station data and updating user default stations.
 */
import com.user.demo.model.Station;
import com.user.demo.model.User;
import com.user.demo.repository.StationRepository;
import com.user.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StationSyncService {
    @Autowired
    private StationRepository stationRepository;
    
    @Autowired
    private UserRepository userRepository;

    /**
     * Sync station data from external source and update user default stations.
     * Uses batch operations to minimize database calls.
     */
    @Transactional
    public void syncStations() {
        Map<String, Object> stationData = fetchStationsData();
        List<Map<String, Object>> dataList = (List<Map<String, Object>>) stationData.get("data");
        if (dataList == null) return;

        // Extract all station codes and usernames
        Set<String> stationCodes = dataList.stream()
            .map(record -> (String) record.get("STATION_CODE"))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Set<String> usernames = dataList.stream()
            .map(record -> {
                String email = (String) record.get("EMAIL");
                return email != null ? email.split("@")[0] : null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        // Get existing stations in one query
        Set<String> existingStations = stationRepository.findAllById(stationCodes)
            .stream()
            .map(Station::getCode)
            .collect(Collectors.toSet());

        // Create new stations in batch
        List<Station> newStations = stationCodes.stream()
            .filter(code -> !existingStations.contains(code))
            .map(code -> {
                Station station = new Station();
                station.setCode(code);
                return station;
            })
            .collect(Collectors.toList());

        if (!newStations.isEmpty()) {
            stationRepository.saveAll(newStations);
        }

        // Get all users in one query
        Map<String, User> users = userRepository.findAllById(usernames)
            .stream()
            .collect(Collectors.toMap(User::getUsername, user -> user));

        // Update users in batch
        List<User> updatedUsers = new ArrayList<>();
        for (Map<String, Object> record : dataList) {
            String stationCode = (String) record.get("STATION_CODE");
            String email = (String) record.get("EMAIL");
            if (stationCode == null || email == null) continue;

            String username = email.split("@")[0];
            User user = users.get(username);
            if (user != null) {
                user.setStationDefault(stationCode);
                updatedUsers.add(user);
            }
        }

        if (!updatedUsers.isEmpty()) {
            userRepository.saveAll(updatedUsers);
        }
    }

    /**
     * Simulate fetching station data from an external API.
     */
    private Map<String, Object> fetchStationsData() {
        Map<String, Object> result = new HashMap<>();
        result.put("total", 108720);
        result.put("pages", 1088);
        result.put("totalRow", 0);
        List<Map<String, Object>> data = new ArrayList<>();
        // Record 1
        Map<String, Object> rec1 = new HashMap<>();
        rec1.put("STT", "1");
        rec1.put("STATION_ID", "36790");
        rec1.put("STATION_CODE", "GLI0194");
        rec1.put("NATIONAL_CODE", "VNM");
        rec1.put("USER_ID", "42435");
        rec1.put("USERNAME", "hienlt11");
        rec1.put("DEPLOY_CODE", "074728");
        rec1.put("FULL_NAME", "Lê Thanh Hiền");
        rec1.put("PHONE", "0975257979");
        rec1.put("STATUS", "1");
        rec1.put("EMAIL", "hienlt11@viettel.com.vn");
        rec1.put("STAFF_TYPE", "1");
        rec1.put("totalRow", 108720);
        data.add(rec1);
        // Record 2
        Map<String, Object> rec2 = new HashMap<>();
        rec2.put("STT", "2");
        rec2.put("STATION_ID", "69601");
        rec2.put("STATION_CODE", "GLI0193-13");
        rec2.put("NATIONAL_CODE", "VNM");
        rec2.put("USER_ID", "42435");
        rec2.put("USERNAME", "hienlt11");
        rec2.put("DEPLOY_CODE", "074728");
        rec2.put("FULL_NAME", "Lê Thanh Hiền");
        rec2.put("PHONE", "0975257979");
        rec2.put("STATUS", "1");
        rec2.put("EMAIL", "hienlt11@viettel.com.vn");
        rec2.put("STAFF_TYPE", "1");
        rec2.put("totalRow", 108720);
        data.add(rec2);
        // Record 3
        Map<String, Object> rec3 = new HashMap<>();
        rec3.put("STT", "3");
        rec3.put("STATION_ID", "42613");
        rec3.put("STATION_CODE", "GLI0195");
        rec3.put("NATIONAL_CODE", "VNM");
        rec3.put("USER_ID", "42435");
        rec3.put("USERNAME", "hienlt11");
        rec3.put("DEPLOY_CODE", "074728");
        rec3.put("FULL_NAME", "Lê Thanh Hiền");
        rec3.put("PHONE", "0975257979");
        rec3.put("STATUS", "1");
        rec3.put("EMAIL", "hienlt11@viettel.com.vn");
        rec3.put("STAFF_TYPE", "1");
        rec3.put("totalRow", 108720);
        data.add(rec3);
        result.put("data", data);
        result.put("key", "SUCCESS");
        return result;
    }
} 