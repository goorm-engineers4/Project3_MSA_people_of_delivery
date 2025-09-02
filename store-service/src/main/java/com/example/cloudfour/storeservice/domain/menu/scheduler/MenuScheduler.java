package com.example.cloudfour.storeservice.domain.menu.scheduler;

import com.example.cloudfour.storeservice.domain.menu.repository.MenuOptionRepository;
import com.example.cloudfour.storeservice.domain.menu.repository.MenuRepository;
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
public class MenuScheduler {

    private final MenuRepository menuRepository;
    private final MenuOptionRepository menuOptionRepository;

    @Scheduled(cron = "0 0 4 * * *")
    public void deleteMenu(){
        LocalDateTime oneDays = LocalDateTime.now().minusDays(1);
        menuRepository.deleteAllByDeletedAtBefore(oneDays);
        log.info("Soft Deleted된 Store 삭제");
    }

    @Scheduled(cron = "0 0 4 * * *")
    public void deleteMenuOption() {
        LocalDateTime oneDays = LocalDateTime.now().minusDays(1);
        menuOptionRepository.deleteAllByDeletedAtBefore(oneDays);
        log.info("Soft Deleted된 Store 삭제");
    }
}
