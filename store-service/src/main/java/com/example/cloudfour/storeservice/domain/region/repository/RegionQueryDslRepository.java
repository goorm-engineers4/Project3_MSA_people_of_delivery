package com.example.cloudfour.storeservice.domain.region.repository;

import com.example.cloudfour.storeservice.domain.region.entity.Region;

import java.util.Optional;

public interface RegionQueryDslRepository {
    Optional<Region> findBySiDoAndSiGunGuAndEupMyeonDong(String siDo, String siGunGu, String eupMyeonDong);
}
