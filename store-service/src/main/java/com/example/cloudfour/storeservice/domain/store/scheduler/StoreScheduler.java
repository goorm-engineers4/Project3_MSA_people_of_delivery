package com.example.cloudfour.storeservice.domain.store.scheduler;

import com.example.cloudfour.storeservice.domain.store.repository.StoreRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class StoreScheduler {

    private final StoreRepository storeRepository;

    @Scheduled(cron = "0 * * * * *")
    public void deleteStore(){
        LocalDateTime oneDays = LocalDateTime.now().minusDays(1);
        storeRepository.deleteAllByDeletedAtBefore(oneDays);
        log.info("Soft Deleted된 Store 삭제");
    }
}
