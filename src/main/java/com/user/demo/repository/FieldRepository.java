package com.user.demo.repository;

/**
 * Repository for Field entities.
 */
import com.user.demo.model.Field;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FieldRepository extends JpaRepository<Field, Long> {
} 