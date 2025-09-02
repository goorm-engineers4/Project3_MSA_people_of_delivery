package com.example.cloudfour.storeservice.domain.store.repository;

import com.example.cloudfour.storeservice.domain.store.entity.StoreCategory;
import com.example.cloudfour.storeservice.domain.store.repository.querydsl.StoreCategoryQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StoreCategoryRepository extends JpaRepository<StoreCategory, UUID>, StoreCategoryQueryDslRepository {

}
