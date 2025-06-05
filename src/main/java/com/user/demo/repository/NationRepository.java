package com.user.demo.repository;

/**
 * Repository for Nation entities.
 */
import com.user.demo.model.Nation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NationRepository extends JpaRepository<Nation, String> {
} 