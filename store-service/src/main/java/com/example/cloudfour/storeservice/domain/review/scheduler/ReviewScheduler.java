package com.example.cloudfour.storeservice.domain.review.scheduler;

import com.example.cloudfour.storeservice.domain.review.repository.ReviewRepository;
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
public class ReviewScheduler {

    private final ReviewRepository reviewRepository;

    @Scheduled(cron = "0 0 4 * * *")
    public void deleteReview(){
        LocalDateTime oneDays = LocalDateTime.now().minusDays(1);
        reviewRepository.deleteAllByDeletedAtBefore(oneDays);
        log.info("Soft Deleted된 Review 삭제");
    }
}
