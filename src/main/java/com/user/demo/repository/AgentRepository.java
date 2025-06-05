package com.user.demo.repository;

/**
 * Repository for Agent entities.
 */
import com.user.demo.model.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {
} 