package com.example.cloudfour.storeservice.domain.review.service.query;

import com.example.cloudfour.modulecommon.dto.Passport;
import com.example.cloudfour.storeservice.client.UserClient;
import com.example.cloudfour.storeservice.domain.collection.document.ReviewDocument;
import com.example.cloudfour.storeservice.domain.collection.repository.query.ReviewSearchRepository;
import com.example.cloudfour.storeservice.domain.common.UserResponseDTO;
import com.example.cloudfour.storeservice.domain.review.dto.ReviewResponseDTO;
import com.example.cloudfour.storeservice.domain.review.exception.ReviewErrorCode;
import com.example.cloudfour.storeservice.domain.review.exception.ReviewException;
import com.example.cloudfour.storeservice.domain.store.entity.Store;
import com.example.cloudfour.storeservice.domain.store.exception.StoreErrorCode;
import com.example.cloudfour.storeservice.domain.store.exception.StoreException;
import com.example.cloudfour.storeservice.domain.store.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewQueryServiceTest {

    @Mock private ReviewSearchRepository reviewRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private UserClient userClient;

    @InjectMocks private ReviewQueryService reviewQueryService;

    private UUID userId;
    private Passport passport;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        passport = Passport.builder().userId(userId).build();
    }

    private ReviewDocument sampleDoc(UUID reviewId, UUID storeId, String userName, LocalDateTime createdAt) {
        return ReviewDocument.builder()
                .reviewId(reviewId)
                .userId(userId)
                .storeId(storeId)
                .userName(userName)
                .score(4.5f)
                .content("맛있어요")
                .pictureUrl("pic")
                .createdAt(createdAt)
                .build();
    }

    @Test
    @DisplayName("getReviewById: 성공 - 상세 조회 반환")
    void getReviewById_success() {
        UUID reviewId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        given(userClient.getUser(userId)).willReturn(UserResponseDTO.builder().userId(userId).nickname("닉네임").build());
        ReviewDocument doc = sampleDoc(reviewId, storeId, "리뷰작성자", LocalDateTime.now());
        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(doc));

        ReviewResponseDTO.ReviewDetailResponseDTO res = reviewQueryService.getReviewById(reviewId, passport);

        assertThat(res.getStoreId()).isEqualTo(storeId);
        assertThat(res.getUserId()).isEqualTo(userId);
        assertThat(res.getNickname()).isEqualTo("리뷰작성자");
        assertThat(res.getReviewCommonGetResponseDTO().getScore()).isEqualTo(4.5f);
        assertThat(res.getReviewCommonGetResponseDTO().getContent()).isEqualTo("맛있어요");
    }

    @Test
    @DisplayName("getReviewById: 권한 없음 -> UNAUTHORIZED_ACCESS")
    void getReviewById_unauthorized() {
        assertThatThrownBy(() -> reviewQueryService.getReviewById(UUID.randomUUID(), null))
                .isInstanceOf(ReviewException.class)
                .hasMessageContaining(ReviewErrorCode.UNAUTHORIZED_ACCESS.getMessage());
        verifyNoInteractions(userClient, reviewRepository);
    }

    @Test
    @DisplayName("getReviewById: 사용자 조회 실패 -> UNAUTHORIZED_ACCESS")
    void getReviewById_userNotFound() {
        given(userClient.getUser(userId)).willReturn(null);
        assertThatThrownBy(() -> reviewQueryService.getReviewById(UUID.randomUUID(), passport))
                .isInstanceOf(ReviewException.class)
                .hasMessageContaining(ReviewErrorCode.UNAUTHORIZED_ACCESS.getMessage());
        verify(reviewRepository, never()).findById(any());
    }

    @Test
    @DisplayName("getReviewById: 리뷰 없음 -> NOT_FOUND")
    void getReviewById_notFound() {
        given(userClient.getUser(userId)).willReturn(UserResponseDTO.builder().userId(userId).nickname("n").build());
        UUID reviewId = UUID.randomUUID();
        given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());
        assertThatThrownBy(() -> reviewQueryService.getReviewById(reviewId, passport))
                .isInstanceOf(ReviewException.class)
                .hasMessageContaining(ReviewErrorCode.NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("getReviewListByStore: 성공 - 페이징/커서 동작")
    void getReviewListByStore_success() {
        UUID storeId = UUID.randomUUID();
        given(storeRepository.findById(storeId)).willReturn(Optional.of(mock(Store.class)));
        LocalDateTime now = LocalDateTime.now();
        ReviewDocument d1 = sampleDoc(UUID.randomUUID(), storeId, "u1", now.minusMinutes(3));
        ReviewDocument d2 = sampleDoc(UUID.randomUUID(), storeId, "u2", now.minusMinutes(1));
        Slice<ReviewDocument> slice = new SliceImpl<>(List.of(d1, d2), PageRequest.of(0, 2), true);
        given(reviewRepository.findAllByStoreId(eq(storeId), any(), any())).willReturn(slice);

        ReviewResponseDTO.ReviewStoreListResponseDTO res = reviewQueryService.getReviewListByStore(storeId, null, 2, passport);

        assertThat(res.getReviews()).hasSize(2);
        assertThat(res.isHasNext()).isTrue();
        assertThat(res.getCursor()).isEqualTo(d2.getCreatedAt());
    }

    @Test
    @DisplayName("getReviewListByStore: 가게 없음 -> NOT_FOUND")
    void getReviewListByStore_storeNotFound() {
        UUID storeId = UUID.randomUUID();
        given(storeRepository.findById(storeId)).willReturn(Optional.empty());
        assertThatThrownBy(() -> reviewQueryService.getReviewListByStore(storeId, LocalDateTime.now(), 10, passport))
                .isInstanceOf(StoreException.class)
                .hasMessageContaining(StoreErrorCode.NOT_FOUND.getMessage());
        verify(reviewRepository, never()).findAllByStoreId(any(), any(), any());
    }

    @Test
    @DisplayName("getReviewListByStore: 권한 없음 -> UNAUTHORIZED_ACCESS")
    void getReviewListByStore_unauthorized() {
        UUID storeId = UUID.randomUUID();
        given(storeRepository.findById(storeId)).willReturn(Optional.of(mock(Store.class)));
        assertThatThrownBy(() -> reviewQueryService.getReviewListByStore(storeId, LocalDateTime.now(), 10, null))
                .isInstanceOf(ReviewException.class)
                .hasMessageContaining(ReviewErrorCode.UNAUTHORIZED_ACCESS.getMessage());
        verify(reviewRepository, never()).findAllByStoreId(any(), any(), any());
    }

    @Test
    @DisplayName("getReviewListByStore: 리뷰 없음 -> NOT_FOUND")
    void getReviewListByStore_noReviews() {
        UUID storeId = UUID.randomUUID();
        given(storeRepository.findById(storeId)).willReturn(Optional.of(mock(Store.class)));
        Slice<ReviewDocument> empty = new SliceImpl<>(List.of(), PageRequest.of(0, 10), false);
        given(reviewRepository.findAllByStoreId(eq(storeId), any(), any())).willReturn(empty);

        assertThatThrownBy(() -> reviewQueryService.getReviewListByStore(storeId, LocalDateTime.now(), 10, passport))
                .isInstanceOf(ReviewException.class)
                .hasMessageContaining(ReviewErrorCode.NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("getReviewListByUser: 성공 - 페이징/커서 동작")
    void getReviewListByUser_success() {
        LocalDateTime now = LocalDateTime.now();
        ReviewDocument d1 = sampleDoc(UUID.randomUUID(), UUID.randomUUID(), "u1", now.minusMinutes(2));
        Slice<ReviewDocument> slice = new SliceImpl<>(List.of(d1), PageRequest.of(0, 1), false);
        given(reviewRepository.findAllByUserId(eq(userId), any(), any())).willReturn(slice);

        ReviewResponseDTO.ReviewUserListResponseDTO res = reviewQueryService.getReviewListByUser(null, 1, passport);

        assertThat(res.getReviews()).hasSize(1);
        assertThat(res.isHasNext()).isFalse();
        assertThat(res.getCursor()).isNull();
    }

    @Test
    @DisplayName("getReviewListByUser: 권한 없음 -> UNAUTHORIZED_ACCESS")
    void getReviewListByUser_unauthorized() {
        assertThatThrownBy(() -> reviewQueryService.getReviewListByUser(LocalDateTime.now(), 10, null))
                .isInstanceOf(ReviewException.class)
                .hasMessageContaining(ReviewErrorCode.UNAUTHORIZED_ACCESS.getMessage());
        verify(reviewRepository, never()).findAllByUserId(any(), any(), any());
    }

    @Test
    @DisplayName("getReviewListByUser: 리뷰 없음 -> NOT_FOUND")
    void getReviewListByUser_noReviews() {
        Slice<ReviewDocument> empty = new SliceImpl<>(List.of(), PageRequest.of(0, 10), false);
        given(reviewRepository.findAllByUserId(eq(userId), any(), any())).willReturn(empty);
        assertThatThrownBy(() -> reviewQueryService.getReviewListByUser(LocalDateTime.now(), 10, passport))
                .isInstanceOf(ReviewException.class)
                .hasMessageContaining(ReviewErrorCode.NOT_FOUND.getMessage());
    }
}

