package com.user.demo.repository;

/**
 * Repository for District entities.
 */
import com.user.demo.model.District;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DistrictRepository extends JpaRepository<District, String> {
    List<District> findByProvinceCode(String provinceCode);
} 