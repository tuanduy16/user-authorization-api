package com.user.demo.repository;

import com.user.demo.model.LocationPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationPermissionRepository extends JpaRepository<LocationPermission, String> {
} 