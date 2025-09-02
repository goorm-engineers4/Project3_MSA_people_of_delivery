package com.example.cloudfour.storeservice.domain.menu.repository.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import static com.example.cloudfour.storeservice.domain.menu.entity.QMenu.menu;
import static com.example.cloudfour.storeservice.domain.store.entity.QStore.store;

@Repository
@RequiredArgsConstructor
public class MenuQueryDslRepositoryImpl implements MenuQueryDslRepository{
    private final JPAQueryFactory query;

    @Override
    public boolean existsByNameAndStoreId(String name, UUID storeId) {
        return query.selectFrom(menu).join(menu.store, store).fetchJoin()
                .where(menu.name.eq(name), store.id.eq(storeId)).fetchFirst() != null;
    }
}