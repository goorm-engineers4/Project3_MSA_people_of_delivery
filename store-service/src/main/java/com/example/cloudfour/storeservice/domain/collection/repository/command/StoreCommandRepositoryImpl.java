package com.example.cloudfour.storeservice.domain.collection.repository.command;

import com.example.cloudfour.storeservice.domain.collection.document.StoreDocument;
import com.example.cloudfour.storeservice.domain.store.entity.Store;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Transactional
public class StoreCommandRepositoryImpl implements StoreCustomCommandRepository{

    private final MongoTemplate mongoTemplate;

    @Override
    public void updateStoreByStoreId(UUID storeId, Store store) {
        Query query = Query.query(Criteria.where("storeId").is(storeId));

        Update update = new Update()
                .set("name", store.getName())
                .set("address", store.getAddress());

        mongoTemplate.updateFirst(query, update, StoreDocument.class);
    }
}
