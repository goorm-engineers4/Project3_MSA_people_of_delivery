package com.example.cloudfour.storeservice.domain.store.repository.querydsl;

import com.example.cloudfour.storeservice.domain.store.entity.StoreCategory;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.example.cloudfour.storeservice.domain.store.entity.QStoreCategory.storeCategory;

@Repository
@RequiredArgsConstructor
public class StoreCategoryQueryDslRepositoryImpl implements StoreCategoryQueryDslRepository{

    private final JPAQueryFactory query;

    @Override
    public Optional<StoreCategory> findByCategory(String category) {
        return Optional.ofNullable(query.selectFrom(storeCategory).where(storeCategory.category.eq(category)).fetchOne());
    }
}
