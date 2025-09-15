package com.example.cloudfour.userservice.domain.region.repository;

import com.example.cloudfour.userservice.domain.region.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RegionRepository extends JpaRepository<Region, UUID> {
    Optional<Region> findBySiDoAndSiGunGuAndEupMyeonDong(String siDo, String siGunGu, String eupMyeonDong);
}
