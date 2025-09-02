package com.example.cloudfour.storeservice.domain.menu.repository.querydsl;

import com.example.cloudfour.storeservice.domain.menu.entity.MenuCategory;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.example.cloudfour.storeservice.domain.menu.entity.QMenuCategory.menuCategory;

@Repository
@RequiredArgsConstructor
public class MenuCategoryQueryDslRepositoryImpl implements MenuCategoryQueryDslRepository{
    private final JPAQueryFactory query;


    @Override
    public Optional<MenuCategory> findByCategory(String category) {
        return Optional.ofNullable(query.selectFrom(menuCategory).where(menuCategory.category.eq(category)).fetchOne());
    }
}
