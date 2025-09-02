package com.example.cloudfour.storeservice.domain.menu.repository.querydsl;

import java.util.UUID;

public interface MenuQueryDslRepository {
    boolean existsByNameAndStoreId(String name, UUID storeId);
}
