package com.user.demo.repository;

/**
 * Repository for Station entities.
 */
import com.user.demo.model.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StationRepository extends JpaRepository<Station, String> {
} 