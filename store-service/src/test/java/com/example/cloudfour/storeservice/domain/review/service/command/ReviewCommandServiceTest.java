package com.example.cloudfour.storeservice.domain.review.service.command;

import com.example.cloudfour.modulecommon.dto.CurrentUser;
import com.example.cloudfour.storeservice.domain.collection.repository.command.ReviewCommandRepository;
import com.example.cloudfour.storeservice.domain.review.converter.ReviewConverter;
import com.example.cloudfour.storeservice.domain.review.dto.ReviewRequestDTO;
import com.example.cloudfour.storeservice.domain.review.dto.ReviewResponseDTO;
import com.example.cloudfour.storeservice.domain.review.entity.Review;
import com.example.cloudfour.storeservice.domain.review.exception.ReviewErrorCode;
import com.example.cloudfour.storeservice.domain.review.exception.ReviewException;
import com.example.cloudfour.storeservice.domain.review.repository.ReviewRepository;
import com.example.cloudfour.storeservice.domain.review.controller.ReviewCommonRequestDTO;
import com.example.cloudfour.storeservice.domain.store.entity.Store;
import com.example.cloudfour.storeservice.domain.store.exception.StoreErrorCode;
import com.example.cloudfour.storeservice.domain.store.exception.StoreException;
import com.example.cloudfour.storeservice.domain.store.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewCommandService 단위테스트")
class ReviewCommandServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewCommandRepository reviewCommandRepository;

    @InjectMocks
    private ReviewCommandService reviewCommandService;

    private UUID userId;
    private UUID storeId;
    private UUID reviewId;
    private CurrentUser currentUser;
    private ReviewRequestDTO.ReviewCreateRequestDTO createRequestDTO;
    private ReviewRequestDTO.ReviewUpdateRequestDTO updateRequestDTO;
    private Review review;
    private Store store;
    private ReviewResponseDTO.ReviewCreateResponseDTO createResponseDTO;
    private ReviewResponseDTO.ReviewUpdateResponseDTO updateResponseDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        reviewId = UUID.randomUUID();
        currentUser = new CurrentUser(userId, "ROLE_USER");

        // Set up ReviewCommonRequestDTO
        ReviewCommonRequestDTO reviewCommonRequestDTO = ReviewCommonRequestDTO.builder()
                .storeId(storeId)
                .score(4.5f)
                .content("Great food and service!")
                .pictureUrl("review.jpg")
                .build();

        // Set up ReviewCreateRequestDTO
        createRequestDTO = ReviewRequestDTO.ReviewCreateRequestDTO.builder()
                .reviewCommonRequestDTO(reviewCommonRequestDTO)
                .build();

        // Set up ReviewUpdateRequestDTO
        updateRequestDTO = ReviewRequestDTO.ReviewUpdateRequestDTO.builder()
                .reviewCommonRequestDTO(reviewCommonRequestDTO)
                .build();

        // Set up Store
        store = mock(Store.class);
        lenient().when(store.getId()).thenReturn(storeId);

        // Set up Review
        review = mock(Review.class);
        lenient().when(review.getId()).thenReturn(reviewId);
        lenient().when(review.getScore()).thenReturn(4.5f);
        lenient().when(review.getContent()).thenReturn("Great food and service!");
        lenient().when(review.getPictureUrl()).thenReturn("review.jpg");
        lenient().when(review.getUser()).thenReturn(userId);
        lenient().when(review.getCreatedAt()).thenReturn(LocalDateTime.now());
        lenient().when(review.getUpdatedAt()).thenReturn(LocalDateTime.now());

        // Set up response DTOs
        createResponseDTO = mock(ReviewResponseDTO.ReviewCreateResponseDTO.class);
        updateResponseDTO = mock(ReviewResponseDTO.ReviewUpdateResponseDTO.class);
    }

    @Nested
    @DisplayName("createReview 메소드는")
    class CreateReviewTests {

        @Test
        @DisplayName("유효한 요청이 주어지면 리뷰를 생성하고 응답을 반환한다")
        void createReview_ValidRequest_ReturnsResponse() {
            // Given
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
            
            try (MockedStatic<ReviewConverter> mockedStatic = mockStatic(ReviewConverter.class)) {
                mockedStatic.when(() -> ReviewConverter.toReview(createRequestDTO)).thenReturn(review);
                mockedStatic.when(() -> ReviewConverter.toReviewCreateResponseDTO(review)).thenReturn(createResponseDTO);

                // When
                ReviewResponseDTO.ReviewCreateResponseDTO result = reviewCommandService.createReview(createRequestDTO, currentUser);

                // Then
                assertThat(result).isEqualTo(createResponseDTO);
                verify(storeRepository).findById(storeId);
                verify(review).setUser(userId);
                verify(review).setStore(store);
                verify(reviewRepository).save(review);
            }
        }

        @Test
        @DisplayName("사용자가 null이면 예외를 던진다")
        void createReview_NullUser_ThrowsException() {
            // Given
            CurrentUser nullUser = null;

            // When & Then
            assertThatThrownBy(() -> reviewCommandService.createReview(createRequestDTO, nullUser))
                    .isInstanceOf(ReviewException.class)
                    .hasFieldOrPropertyWithValue("code", ReviewErrorCode.UNAUTHORIZED_ACCESS);
            
            verify(storeRepository, never()).findById(any());
            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("가게를 찾을 수 없으면 예외를 던진다")
        void createReview_StoreNotFound_ThrowsException() {
            // Given
            when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> reviewCommandService.createReview(createRequestDTO, currentUser))
                    .isInstanceOf(StoreException.class)
                    .hasFieldOrPropertyWithValue("code", StoreErrorCode.NOT_FOUND);
            
            verify(storeRepository).findById(storeId);
            verify(reviewRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateReview 메소드는")
    class UpdateReviewTests {

        @Test
        @DisplayName("유효한 요청이 주어지면 리뷰를 수정하고 응답을 반환한다")
        void updateReview_ValidRequest_ReturnsResponse() {
            // Given
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
            when(reviewRepository.existsByReviewIdAndUserId(reviewId, userId)).thenReturn(true);
            
            try (MockedStatic<ReviewConverter> mockedStatic = mockStatic(ReviewConverter.class)) {
                mockedStatic.when(() -> ReviewConverter.toReviewUpdateResponseDTO(review)).thenReturn(updateResponseDTO);

                // When
                ReviewResponseDTO.ReviewUpdateResponseDTO result = reviewCommandService.updateReview(updateRequestDTO, reviewId, currentUser);

                // Then
                assertThat(result).isEqualTo(updateResponseDTO);
                verify(storeRepository).findById(storeId);
                verify(reviewRepository).findById(reviewId);
                verify(reviewRepository).existsByReviewIdAndUserId(reviewId, userId);
                verify(review).update(updateRequestDTO);
                verify(review).setStore(store);
            }
        }

        @Test
        @DisplayName("가게를 찾을 수 없으면 예외를 던진다")
        void updateReview_StoreNotFound_ThrowsException() {
            // Given
            when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> reviewCommandService.updateReview(updateRequestDTO, reviewId, currentUser))
                    .isInstanceOf(StoreException.class)
                    .hasFieldOrPropertyWithValue("code", StoreErrorCode.NOT_FOUND);
            
            verify(storeRepository).findById(storeId);
            verify(reviewRepository, never()).findById(any());
        }

        @Test
        @DisplayName("리뷰를 찾을 수 없으면 예외를 던진다")
        void updateReview_ReviewNotFound_ThrowsException() {
            // Given
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> reviewCommandService.updateReview(updateRequestDTO, reviewId, currentUser))
                    .isInstanceOf(ReviewException.class)
                    .hasFieldOrPropertyWithValue("code", ReviewErrorCode.NOT_FOUND);
            
            verify(storeRepository).findById(storeId);
            verify(reviewRepository).findById(reviewId);
        }

        @Test
        @DisplayName("사용자가 null이면 예외를 던진다")
        void updateReview_NullUser_ThrowsException() {
            // Given
            CurrentUser nullUser = null;
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

            // When & Then
            assertThatThrownBy(() -> reviewCommandService.updateReview(updateRequestDTO, reviewId, nullUser))
                    .isInstanceOf(ReviewException.class)
                    .hasFieldOrPropertyWithValue("code", ReviewErrorCode.UNAUTHORIZED_ACCESS);
            
            verify(storeRepository).findById(storeId);
            verify(reviewRepository).findById(reviewId);
        }

        @Test
        @DisplayName("사용자가 리뷰 작성자가 아니면 예외를 던진다")
        void updateReview_UnauthorizedUser_ThrowsException() {
            // Given
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
            when(reviewRepository.existsByReviewIdAndUserId(reviewId, userId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> reviewCommandService.updateReview(updateRequestDTO, reviewId, currentUser))
                    .isInstanceOf(ReviewException.class)
                    .hasFieldOrPropertyWithValue("code", ReviewErrorCode.UNAUTHORIZED_ACCESS);
            
            verify(storeRepository).findById(storeId);
            verify(reviewRepository).findById(reviewId);
            verify(reviewRepository).existsByReviewIdAndUserId(reviewId, userId);
        }
    }

    @Nested
    @DisplayName("deleteReview 메소드는")
    class DeleteReviewTests {

        @Test
        @DisplayName("유효한 요청이 주어지면 리뷰를 삭제한다")
        void deleteReview_ValidRequest_DeletesReview() {
            // Given
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
            when(reviewRepository.existsByReviewIdAndUserId(reviewId, userId)).thenReturn(true);

            // When
            reviewCommandService.deleteReview(reviewId, currentUser);

            // Then
            verify(reviewRepository).findById(reviewId);
            verify(reviewRepository).existsByReviewIdAndUserId(reviewId, userId);
            verify(review).softDelete();
        }

        @Test
        @DisplayName("리뷰를 찾을 수 없으면 예외를 던진다")
        void deleteReview_ReviewNotFound_ThrowsException() {
            // Given
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> reviewCommandService.deleteReview(reviewId, currentUser))
                    .isInstanceOf(ReviewException.class)
                    .hasFieldOrPropertyWithValue("code", ReviewErrorCode.NOT_FOUND);
            
            verify(reviewRepository).findById(reviewId);
        }

        @Test
        @DisplayName("사용자가 null이면 예외를 던진다")
        void deleteReview_NullUser_ThrowsException() {
            // Given
            CurrentUser nullUser = null;
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

            // When & Then
            assertThatThrownBy(() -> reviewCommandService.deleteReview(reviewId, nullUser))
                    .isInstanceOf(ReviewException.class)
                    .hasFieldOrPropertyWithValue("code", ReviewErrorCode.UNAUTHORIZED_ACCESS);
            
            verify(reviewRepository).findById(reviewId);
        }

        @Test
        @DisplayName("사용자가 리뷰 작성자가 아니면 예외를 던진다")
        void deleteReview_UnauthorizedUser_ThrowsException() {
            // Given
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
            when(reviewRepository.existsByReviewIdAndUserId(reviewId, userId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> reviewCommandService.deleteReview(reviewId, currentUser))
                    .isInstanceOf(ReviewException.class)
                    .hasFieldOrPropertyWithValue("code", ReviewErrorCode.UNAUTHORIZED_ACCESS);
            
            verify(reviewRepository).findById(reviewId);
            verify(reviewRepository).existsByReviewIdAndUserId(reviewId, userId);
        }
    }
}