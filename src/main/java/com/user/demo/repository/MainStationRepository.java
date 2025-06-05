package com.user.demo.repository;

/**
 * Repository for MainStation entities.
 */
import com.user.demo.model.MainStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MainStationRepository extends JpaRepository<MainStation, String> {
} 