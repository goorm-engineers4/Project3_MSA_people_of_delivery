package com.example.cloudfour.storeservice.domain.region.repository;

import com.example.cloudfour.storeservice.domain.region.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RegionRepository extends JpaRepository<Region, UUID> , RegionQueryDslRepository{

}