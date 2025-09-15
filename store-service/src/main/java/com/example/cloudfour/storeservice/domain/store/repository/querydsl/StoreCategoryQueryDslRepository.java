package com.example.cloudfour.storeservice.domain.store.repository.querydsl;

import com.example.cloudfour.storeservice.domain.store.entity.StoreCategory;

import java.util.Optional;

public interface StoreCategoryQueryDslRepository {
    Optional<StoreCategory> findByCategory(String category);
}
