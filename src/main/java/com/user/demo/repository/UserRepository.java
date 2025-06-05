package com.user.demo.repository;

/**
 * Repository for User entities.
 */
import com.user.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
} 