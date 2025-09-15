package com.example.cloudfour.storeservice.domain.store.scheduler;

import com.example.cloudfour.storeservice.domain.collection.converter.DocumentConverter;
import com.example.cloudfour.storeservice.domain.collection.document.StoreDocument;
import com.example.cloudfour.storeservice.domain.collection.repository.command.StoreCommandRepository;
import com.example.cloudfour.storeservice.domain.common.enums.SyncStatus;
import com.example.cloudfour.storeservice.domain.store.entity.Store;
import com.example.cloudfour.storeservice.domain.store.repository.StoreRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class StoreMongoScheduler {

    private final StoreRepository storeRepository;
    private final StoreCommandRepository storeCommandRepository;

    @Scheduled(cron = "0 * * * * *")
    public void syncStoreScheduler(){
        log.info("Review Mongo 동기화 시작");
        deleteStore();
        createStore();
        updateStore();
        log.info("Review Mongo 동기화 완료");
    }

    private void createStore(){
        log.info("MongoDB에 가게 동기화 시작");
        List<Store> stores = storeRepository.findAllBySyncStatus(SyncStatus.CREATED_PENDING);
        if(stores.isEmpty()){
            log.info("MongoDB에 생성할 가게가 존재하지 않음");
            return;
        }
        List<StoreDocument> storeDocuments = stores.stream().map(DocumentConverter::toStoreDocument).toList();
        storeCommandRepository.saveAll(storeDocuments);
        stores.forEach(Store::syncCreated);
        storeRepository.saveAll(stores);
        log.info("MongoDB에 가게 동기화 완료");
    }

    private void updateStore() {
        log.info("MongoDB에 가게 업데이트 동기화 시작");
        List<Store> stores = storeRepository.findAllBySyncStatus(SyncStatus.UPDATED_PENDING);
        if (stores.isEmpty()) {
            log.info("MongoDB에 업데이트할 가게가 존재하지 않음");
            return;
        }

        for (Store store : stores) {
            storeCommandRepository.updateStoreByStoreId(store.getId(), store);
            store.syncUpdated();
        }
        storeRepository.saveAll(stores);
        log.info("MongoDB에 가게 업데이트 완료: {} 건", stores.size());
    }

    private void deleteStore(){
        log.info("MongoDB에 가게 삭제 동기화 시작");
        List<Store> stores = storeRepository.findAllByIsDeleted();
        if(stores.isEmpty()){
            log.info("삭제할 가게 데이터 없음");
            return;
        }
        List<UUID> storeIds = stores.stream().map(Store::getId).toList();
        storeCommandRepository.deleteAllByStoreIdIn(storeIds);
        log.info("MongoDB에 가게 삭제 완료");
    }
}
