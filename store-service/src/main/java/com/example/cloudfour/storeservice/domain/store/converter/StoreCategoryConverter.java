package com.example.cloudfour.storeservice.domain.store.converter;

import com.example.cloudfour.storeservice.domain.store.entity.StoreCategory;

public class StoreCategoryConverter {
    public static StoreCategory toStoreCategory(String categoryName) {
        return StoreCategory.builder()
                .category(categoryName)
                .build();
    }
}
