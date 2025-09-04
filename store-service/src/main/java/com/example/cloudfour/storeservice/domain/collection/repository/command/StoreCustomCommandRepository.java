package com.example.cloudfour.storeservice.domain.collection.repository.command;


import com.example.cloudfour.storeservice.domain.store.entity.Store;

import java.util.UUID;

public interface StoreCustomCommandRepository {
    void updateStoreByStoreId(UUID storeId, Store store);
}
