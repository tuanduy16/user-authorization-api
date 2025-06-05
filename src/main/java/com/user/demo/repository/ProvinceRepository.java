package com.user.demo.repository;

/**
 * Repository for Province entities.
 */
import com.user.demo.model.Province;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProvinceRepository extends JpaRepository<Province, String> {
    List<Province> findByAreaCode(String areaCode);
} 