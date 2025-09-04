package com.example.cloudfour.storeservice.domain.review.converter;

import com.example.cloudfour.storeservice.domain.collection.document.ReviewDocument;
import com.example.cloudfour.storeservice.domain.review.controller.ReviewCommonResponseDTO;
import com.example.cloudfour.storeservice.domain.review.dto.ReviewRequestDTO;
import com.example.cloudfour.storeservice.domain.review.dto.ReviewResponseDTO;
import com.example.cloudfour.storeservice.domain.review.entity.Review;

import java.time.LocalDateTime;
import java.util.List;


public class ReviewConverter {
    public static Review toReview(ReviewRequestDTO.ReviewCreateRequestDTO reviewCreateRequestDTO){
        return Review.builder()
                .score(reviewCreateRequestDTO.getReviewCommonRequestDTO().getScore())
                .content(reviewCreateRequestDTO.getReviewCommonRequestDTO().getContent())
                .pictureUrl(reviewCreateRequestDTO.getReviewCommonRequestDTO().getPictureUrl())
                .build();
    }

    public static ReviewResponseDTO.ReviewDetailResponseDTO toReviewDetailResponseDTO(ReviewDocument reviewDocument, String nickname){
        return ReviewResponseDTO.ReviewDetailResponseDTO.builder()
                .storeId(reviewDocument.getStoreId())
                .userId(reviewDocument.getUserId())
                .nickname(nickname)
                .reviewCommonGetResponseDTO(toReviewCommonGetResponseDTO(reviewDocument))
                .createdAt(reviewDocument.getCreatedAt())
                .build();
    }

    public static ReviewResponseDTO.ReviewStoreResponseDTO toReviewStoreResponseDTO(ReviewDocument reviewDocument){
        return ReviewResponseDTO.ReviewStoreResponseDTO.builder()
                .reviewId(reviewDocument.getReviewId())
                .reviewCommonCrudResponseDTO(documentToReviewCommonCrudResponseDTO(reviewDocument))
                .createdAt(reviewDocument.getCreatedAt())
                .createdBy(reviewDocument.getUserId())
                .build();
    }

    public static ReviewResponseDTO.ReviewStoreListResponseDTO toReviewStoreListResponseDTO(List<ReviewResponseDTO.ReviewStoreResponseDTO> reviews, Boolean hasNext, LocalDateTime cursor) {
        return ReviewResponseDTO.ReviewStoreListResponseDTO.builder()
                .reviews(reviews)
                .hasNext(hasNext)
                .cursor(cursor)
                .build();
    }

    public static ReviewResponseDTO.ReviewUserResponseDTO toReviewUserResponseDTO(ReviewDocument reviewDocument){
        return ReviewResponseDTO.ReviewUserResponseDTO.builder()
                .reviewCommonGetResponseDTO(toReviewCommonGetResponseDTO(reviewDocument))
                .createdAt(reviewDocument.getCreatedAt())
                .createdBy(reviewDocument.getUserId())
                .build();
    }

    public static ReviewResponseDTO.ReviewUserListResponseDTO toReviewUserListResponseDTO(List<ReviewResponseDTO.ReviewUserResponseDTO> reviews, Boolean hasNext, LocalDateTime cursor) {
        return ReviewResponseDTO.ReviewUserListResponseDTO.builder()
                .reviews(reviews)
                .hasNext(hasNext)
                .cursor(cursor)
                .build();
    }

    public static ReviewResponseDTO.ReviewCreateResponseDTO toReviewCreateResponseDTO(Review review){
        return ReviewResponseDTO.ReviewCreateResponseDTO.builder()
                .reviewId(review.getId())
                .storeId(review.getStore().getId())
                .reviewCommonCrudResponseDTO(toReviewCommonCrudResponseDTO(review))
                .createdAt(review.getCreatedAt())
                .createdBy(review.getUser())
                .build();
    }

    public static ReviewResponseDTO.ReviewUpdateResponseDTO toReviewUpdateResponseDTO(Review review){
        return ReviewResponseDTO.ReviewUpdateResponseDTO.builder()
                .reviewCommonCrudResponseDTO(toReviewCommonCrudResponseDTO(review))
                .updatedAt(review.getUpdatedAt())
                .updatedBy(review.getUser())
                .build();
    }

    public static ReviewCommonResponseDTO.ReviewCommonGetResponseDTO toReviewCommonGetResponseDTO(ReviewDocument reviewDocument){
        return ReviewCommonResponseDTO.ReviewCommonGetResponseDTO.builder()
                .reviewId(reviewDocument.getReviewId())
                .score(reviewDocument.getScore())
                .content(reviewDocument.getContent())
                .pictureUrl(reviewDocument.getPictureUrl())
                .build();
    }

    public static ReviewCommonResponseDTO.ReviewCommonCrudResponseDTO toReviewCommonCrudResponseDTO(Review review){
        return ReviewCommonResponseDTO.ReviewCommonCrudResponseDTO.builder()
                .userId(review.getUser())
                .score(review.getScore())
                .content(review.getContent())
                .pictureUrl(review.getPictureUrl())
                .build();
    }

    public static ReviewCommonResponseDTO.ReviewCommonCrudResponseDTO documentToReviewCommonCrudResponseDTO(ReviewDocument reviewDocument){
        return ReviewCommonResponseDTO.ReviewCommonCrudResponseDTO.builder()
                .userId(reviewDocument.getUserId())
                .score(reviewDocument.getScore())
                .content(reviewDocument.getContent())
                .pictureUrl(reviewDocument.getPictureUrl())
                .build();
    }
}

