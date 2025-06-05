package com.user.demo.repository;

/**
 * Repository for Area entities.
 */
import com.user.demo.model.Area;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AreaRepository extends JpaRepository<Area, String> {
} 