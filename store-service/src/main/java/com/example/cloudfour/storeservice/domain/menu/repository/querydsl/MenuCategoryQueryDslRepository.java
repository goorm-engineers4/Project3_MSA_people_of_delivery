package com.example.cloudfour.storeservice.domain.menu.repository.querydsl;

import com.example.cloudfour.storeservice.domain.menu.entity.MenuCategory;

import java.util.Optional;

public interface MenuCategoryQueryDslRepository {
    Optional<MenuCategory> findByCategory(String category);
}
