package com.example.cloudfour.storeservice.domain.menu.repository;

import com.example.cloudfour.storeservice.domain.menu.entity.MenuCategory;
import com.example.cloudfour.storeservice.domain.menu.repository.querydsl.MenuCategoryQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, UUID>, MenuCategoryQueryDslRepository {

}
