package com.example.cloudfour.storeservice.domain.review.scheduler;

import com.example.cloudfour.storeservice.domain.collection.converter.DocumentConverter;
import com.example.cloudfour.storeservice.domain.collection.document.ReviewDocument;
import com.example.cloudfour.storeservice.domain.collection.repository.command.ReviewCommandRepository;
import com.example.cloudfour.storeservice.domain.common.enums.SyncStatus;
import com.example.cloudfour.storeservice.domain.review.entity.Review;
import com.example.cloudfour.storeservice.domain.review.repository.ReviewRepository;
import com.example.cloudfour.storeservice.domain.store.entity.Store;
import com.example.cloudfour.storeservice.domain.store.repository.StoreRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class ReviewMongoScheduler {

    private final ReviewRepository reviewRepository;
    private final ReviewCommandRepository reviewCommandRepository;
    private final StoreRepository storeRepository;

    @Scheduled(cron = "0 * * * * *")
    public void syncReviewScheduler(){
        log.info("Review Mongo 동기화 시작");
        deleteReview();
        createReview();
        updateReview();
        refreshReviews();
        log.info("Review Mongo 동기화 완료");
    }

    private void createReview(){
        log.info("MongoDB에 리뷰 동기화 시작");
        List<Review> reviews = reviewRepository.findAllBySyncStatus(SyncStatus.CREATED_PENDING);
        if(reviews.isEmpty()){
            log.info("MongoDB에 생성할 리뷰가 존재하지 않음");
            return;
        }
        List<ReviewDocument> reviewDocuments = reviews.stream().map(DocumentConverter::toReviewDocument).toList();
        reviewCommandRepository.saveAll(reviewDocuments);
        reviews.forEach(Review::syncCreated);
        reviewRepository.saveAll(reviews);
        log.info("MongoDB에 리뷰 동기화 완료");
    }

    private void updateReview() {
        log.info("MongoDB에 리뷰 업데이트 동기화 시작");
        List<Review> reviews = reviewRepository.findAllBySyncStatus(SyncStatus.UPDATED_PENDING);
        if (reviews.isEmpty()) {
            log.info("MongoDB에 업데이트할 리뷰가 존재하지 않음");
            return;
        }

        for (Review review : reviews) {
            reviewCommandRepository.updateReviewByReviewId(review.getId(), review);
            review.syncUpdated();
        }
        reviewRepository.saveAll(reviews);
        log.info("MongoDB에 리뷰 업데이트 완료: {} 건", reviews.size());
    }


    private void deleteReview(){
        log.info("MongoDB에 리뷰 삭제 동기화 시작");
        List<Review> reviews = reviewRepository.findAllByIsDeleted();
        if(reviews.isEmpty()){
            log.info("삭제할 리뷰 데이터 없음");
            return;
        }
        List<UUID> reviewIds = reviews.stream().map(Review::getId).toList();
        reviewCommandRepository.deleteAllByReviewIdIn(reviewIds);
        log.info("MongoDB에 리뷰 삭제 완료");
    }

    private void refreshReviews(){
        log.info("MongoDB에 리뷰 최신화 시작");
        Pageable pageable = PageRequest.of(0,3);
        List<Store> stores = storeRepository.findAllByIsDeletedIsFalse();
        for(Store store:stores){
            Slice<Review> top3Review = reviewRepository.findAllTopThreeReview(store.getId(),pageable);
            List<Review> reviews = top3Review.toList();
            reviewCommandRepository.createReviewByStoreId(store.getId(), reviews);
            reviewCommandRepository.updateStoreReview(store.getId(),store.getReviewCount(),store.getRating());
            log.info("MongoDB에 리뷰 최신화 완료");
        }
    }
}
