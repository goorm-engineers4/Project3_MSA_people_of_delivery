package com.example.cloudfour.storeservice.domain.review.service.command;

import com.example.cloudfour.modulecommon.dto.Passport;
import com.example.cloudfour.storeservice.domain.region.entity.Region;
import com.example.cloudfour.storeservice.domain.review.controller.ReviewCommonRequestDTO;
import com.example.cloudfour.storeservice.domain.review.dto.ReviewRequestDTO;
import com.example.cloudfour.storeservice.domain.review.dto.ReviewResponseDTO;
import com.example.cloudfour.storeservice.domain.review.entity.Review;
import com.example.cloudfour.storeservice.domain.review.exception.ReviewErrorCode;
import com.example.cloudfour.storeservice.domain.review.exception.ReviewException;
import com.example.cloudfour.storeservice.domain.review.repository.ReviewRepository;
import com.example.cloudfour.storeservice.domain.store.entity.Store;
import com.example.cloudfour.storeservice.domain.store.entity.StoreCategory;
import com.example.cloudfour.storeservice.domain.store.exception.StoreErrorCode;
import com.example.cloudfour.storeservice.domain.store.exception.StoreException;
import com.example.cloudfour.storeservice.domain.store.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewCommandServiceTest {

    @Mock private StoreRepository storeRepository;
    @Mock private ReviewRepository reviewRepository;

    @InjectMocks private ReviewCommandService reviewCommandService;

    private UUID userId;
    private Passport
            passport;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        passport = Passport.builder().userId(userId).build();
    }

    private Store sampleStore() {
        return Store.builder()
                .name("store")
                .address("서울 강남구 역삼동")
                .phone("010-1234-5678")
                .content("desc")
                .minPrice(1000)
                .deliveryTip(100)
                .operationHours("09-18")
                .closedDays("sun")
                .storeCategory(StoreCategory.builder().category("KOREAN").build())
                .region(Region.builder().siDo("서울").siGunGu("강남구").eupMyeonDong("역삼동").build())
                .ownerId(UUID.randomUUID())
                .reviewCount(0)
                .build();
    }

    private ReviewRequestDTO.ReviewCreateRequestDTO sampleCreateDTO(UUID storeId, float score, String content, String picture) {
        ReviewCommonRequestDTO common = ReviewCommonRequestDTO.builder()
                .score(score)
                .content(content)
                .pictureUrl(picture)
                .build();
        return ReviewRequestDTO.ReviewCreateRequestDTO.builder()
                .storeId(storeId)
                .reviewCommonRequestDTO(common)
                .build();
    }

    @Test
    @DisplayName("createReview: 성공 - 저장 및 매핑 검증")
    void createReview_success() {
        // given
        UUID storeId = UUID.randomUUID();
        var dto = sampleCreateDTO(storeId, 4.5f, "맛있어요", null);
        Store store = sampleStore();
        given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

        // save는 동일 인스턴스 반환
        given(reviewRepository.save(any(Review.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        ReviewResponseDTO.ReviewCreateResponseDTO res = reviewCommandService.createReview(dto, passport);

        // then
        assertThat(res.getReviewCommonCrudResponseDTO().getUserId()).isEqualTo(userId);
        assertThat(res.getReviewCommonCrudResponseDTO().getScore()).isEqualTo(4.5f);
        assertThat(res.getReviewCommonCrudResponseDTO().getContent()).isEqualTo("맛있어요");
        assertThat(res.getCreatedBy()).isEqualTo(userId);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        Review saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(userId);
        assertThat(saved.getStore()).isEqualTo(store);
    }

    @Test
    @DisplayName("createReview: 권한 없음 -> UNAUTHORIZED_ACCESS")
    void createReview_unauthorized() {
        UUID storeId = UUID.randomUUID();
        var dto = sampleCreateDTO(storeId, 4.0f, "c", null);
        assertThatThrownBy(() -> reviewCommandService.createReview(dto, null))
                .isInstanceOf(ReviewException.class)
                .hasMessageContaining(ReviewErrorCode.UNAUTHORIZED_ACCESS.getMessage());
        verifyNoInteractions(storeRepository, reviewRepository);
    }

    @Test
    @DisplayName("createReview: 가게 없음 -> StoreException NOT_FOUND")
    void createReview_storeNotFound() {
        UUID storeId = UUID.randomUUID();
        var dto = sampleCreateDTO(storeId, 3.0f, "c", null);
        given(storeRepository.findById(storeId)).willReturn(Optional.empty());
        assertThatThrownBy(() -> reviewCommandService.createReview(dto, passport))
                .isInstanceOf(StoreException.class)
                .hasMessageContaining(StoreErrorCode.NOT_FOUND.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateReview: 성공 - 내용/점수 갱신")
    void updateReview_success() {
        UUID reviewId = UUID.randomUUID();
        Store store = sampleStore();
        Review review = Review.builder()
                .score(3.0f)
                .content("old")
                .pictureUrl(null)
                .store(store)
                .user(userId)
                .build();

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
        given(reviewRepository.existsByReviewIdAndUserId(reviewId, userId)).willReturn(true);

        var updateDto = ReviewRequestDTO.ReviewUpdateRequestDTO.builder()
                .reviewCommonRequestDTO(ReviewCommonRequestDTO.builder().score(4.0f).content("new").pictureUrl("pic").build())
                .build();

        // when
        ReviewResponseDTO.ReviewUpdateResponseDTO res = reviewCommandService.updateReview(updateDto, reviewId, passport);

        // then
        assertThat(res.getReviewCommonCrudResponseDTO().getScore()).isEqualTo(4.0f);
        assertThat(res.getReviewCommonCrudResponseDTO().getContent()).isEqualTo("new");
        assertThat(res.getReviewCommonCrudResponseDTO().getPictureUrl()).isEqualTo("pic");
        assertThat(review.getScore()).isEqualTo(4.0f);
        assertThat(review.getContent()).isEqualTo("new");
        assertThat(review.getPictureUrl()).isEqualTo("pic");
    }

    @Test
    @DisplayName("updateReview: 권한 없음 -> UNAUTHORIZED_ACCESS")
    void updateReview_unauthorized() {
        UUID reviewId = UUID.randomUUID();
        Review review = Review.builder()
                .score(3.0f)
                .content("old")
                .store(sampleStore())
                .user(userId)
                .build();
        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
        given(reviewRepository.existsByReviewIdAndUserId(reviewId, userId)).willReturn(false);

        var updateDto = ReviewRequestDTO.ReviewUpdateRequestDTO.builder()
                .reviewCommonRequestDTO(ReviewCommonRequestDTO.builder().score(4.0f).content("new").build())
                .build();

        assertThatThrownBy(() -> reviewCommandService.updateReview(updateDto, reviewId, passport))
                .isInstanceOf(ReviewException.class)
                .hasMessageContaining(ReviewErrorCode.UNAUTHORIZED_ACCESS.getMessage());
    }

    @Test
    @DisplayName("updateReview: 리뷰 없음 -> NOT_FOUND")
    void updateReview_notFound() {
        UUID reviewId = UUID.randomUUID();
        given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());
        var updateDto = ReviewRequestDTO.ReviewUpdateRequestDTO.builder()
                .reviewCommonRequestDTO(ReviewCommonRequestDTO.builder().score(4.0f).content("new").build())
                .build();
        assertThatThrownBy(() -> reviewCommandService.updateReview(updateDto, reviewId, passport))
                .isInstanceOf(ReviewException.class)
                .hasMessageContaining(ReviewErrorCode.NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("deleteReview: 성공 - 소프트 삭제")
    void deleteReview_success() {
        UUID reviewId = UUID.randomUUID();
        Review review = Review.builder()
                .score(5.0f)
                .content("great")
                .store(sampleStore())
                .user(userId)
                .build();
        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
        given(reviewRepository.existsByReviewIdAndUserId(reviewId, userId)).willReturn(true);

        reviewCommandService.deleteReview(reviewId, passport);

        assertThat(review.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("deleteReview: 권한 없음 -> UNAUTHORIZED_ACCESS")
    void deleteReview_unauthorized() {
        UUID reviewId = UUID.randomUUID();
        Review review = Review.builder()
                .score(5.0f)
                .content("great")
                .store(sampleStore())
                .user(userId)
                .build();
        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
        given(reviewRepository.existsByReviewIdAndUserId(reviewId, userId)).willReturn(false);

        assertThatThrownBy(() -> reviewCommandService.deleteReview(reviewId, passport))
                .isInstanceOf(ReviewException.class)
                .hasMessageContaining(ReviewErrorCode.UNAUTHORIZED_ACCESS.getMessage());
    }

    @Test
    @DisplayName("deleteReview: 리뷰 없음 -> NOT_FOUND")
    void deleteReview_notFound() {
        UUID reviewId = UUID.randomUUID();
        given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());
        assertThatThrownBy(() -> reviewCommandService.deleteReview(reviewId, passport))
                .isInstanceOf(ReviewException.class)
                .hasMessageContaining(ReviewErrorCode.NOT_FOUND.getMessage());
    }
}

