package com.example.cloudfour.storeservice.domain.review.service.query;

import com.example.cloudfour.modulecommon.dto.CurrentUser;
import com.example.cloudfour.storeservice.domain.collection.document.ReviewDocument;
import com.example.cloudfour.storeservice.domain.collection.repository.query.ReviewSearchRepository;
import com.example.cloudfour.storeservice.domain.common.UserResponseDTO;
import com.example.cloudfour.storeservice.domain.review.converter.ReviewConverter;
import com.example.cloudfour.storeservice.domain.review.dto.ReviewResponseDTO;
import com.example.cloudfour.storeservice.domain.review.exception.ReviewErrorCode;
import com.example.cloudfour.storeservice.domain.review.exception.ReviewException;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewQueryService 단위테스트")
class ReviewQueryServiceTest {

    @Mock
    private ReviewSearchRepository reviewRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ReviewQueryService reviewQueryService;

    private UUID userId;
    private UUID storeId;
    private UUID reviewId;
    private CurrentUser currentUser;
    private ReviewDocument reviewDocument;
    private Store store;
    private UserResponseDTO userResponseDTO;
    private ReviewResponseDTO.ReviewDetailResponseDTO reviewDetailResponseDTO;
    private ReviewResponseDTO.ReviewStoreResponseDTO reviewStoreResponseDTO;
    private ReviewResponseDTO.ReviewStoreListResponseDTO reviewStoreListResponseDTO;
    private ReviewResponseDTO.ReviewUserResponseDTO reviewUserResponseDTO;
    private ReviewResponseDTO.ReviewUserListResponseDTO reviewUserListResponseDTO;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        reviewId = UUID.randomUUID();
        currentUser = new CurrentUser(userId, "ROLE_USER");
        now = LocalDateTime.now();

        // Set up Store
        store = mock(Store.class);
        lenient().when(store.getId()).thenReturn(storeId);

        // Set up UserResponseDTO
        userResponseDTO = mock(UserResponseDTO.class);
        lenient().when(userResponseDTO.getUserId()).thenReturn(userId);
        lenient().when(userResponseDTO.getNickname()).thenReturn("Test User");

        // Set up ReviewDocument
        reviewDocument = mock(ReviewDocument.class);
        lenient().when(reviewDocument.getReviewId()).thenReturn(reviewId);
        lenient().when(reviewDocument.getScore()).thenReturn(4.5f);
        lenient().when(reviewDocument.getContent()).thenReturn("Great food and service!");
        lenient().when(reviewDocument.getPictureUrl()).thenReturn("review.jpg");
        lenient().when(reviewDocument.getUserId()).thenReturn(userId);
        lenient().when(reviewDocument.getUserName()).thenReturn("Test User");
        lenient().when(reviewDocument.getStoreId()).thenReturn(storeId);
        lenient().when(reviewDocument.getCreatedAt()).thenReturn(now);

        // Set up response DTOs
        reviewDetailResponseDTO = mock(ReviewResponseDTO.ReviewDetailResponseDTO.class);
        reviewStoreResponseDTO = mock(ReviewResponseDTO.ReviewStoreResponseDTO.class);
        reviewStoreListResponseDTO = mock(ReviewResponseDTO.ReviewStoreListResponseDTO.class);
        reviewUserResponseDTO = mock(ReviewResponseDTO.ReviewUserResponseDTO.class);
        reviewUserListResponseDTO = mock(ReviewResponseDTO.ReviewUserListResponseDTO.class);
    }

    @Nested
    @DisplayName("getReviewById 메소드는")
    class GetReviewByIdTests {

        @Test
        @DisplayName("유효한 요청이 주어지면 리뷰 상세 정보를 반환한다")
        void getReviewById_ValidRequest_ReturnsReviewDetail() {
            // Given
            when(restTemplate.getForObject(anyString(), eq(UserResponseDTO.class), any(Object[].class)))
                    .thenReturn(userResponseDTO);
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(reviewDocument));
            
            try (MockedStatic<ReviewConverter> mockedStatic = mockStatic(ReviewConverter.class)) {
                mockedStatic.when(() -> ReviewConverter.toReviewDetailResponseDTO(reviewDocument, "Test User"))
                        .thenReturn(reviewDetailResponseDTO);

                // When
                ReviewResponseDTO.ReviewDetailResponseDTO result = reviewQueryService.getReviewById(reviewId, currentUser);

                // Then
                assertThat(result).isEqualTo(reviewDetailResponseDTO);
                verify(restTemplate).getForObject(anyString(), eq(UserResponseDTO.class), any(Object[].class));
                verify(reviewRepository).findById(reviewId);
            }
        }

        @Test
        @DisplayName("사용자가 null이면 예외를 던진다")
        void getReviewById_NullUser_ThrowsException() {
            // Given
            CurrentUser nullUser = null;

            // When & Then
            assertThatThrownBy(() -> reviewQueryService.getReviewById(reviewId, nullUser))
                    .isInstanceOf(ReviewException.class)
                    .hasFieldOrPropertyWithValue("code", ReviewErrorCode.UNAUTHORIZED_ACCESS);
            
            verify(restTemplate, never()).getForObject(anyString(), any(Class.class), any(Object[].class));
            verify(reviewRepository, never()).findById(any());
        }

        @Test
        @DisplayName("사용자 정보를 찾을 수 없으면 예외를 던진다")
        void getReviewById_UserNotFound_ThrowsException() {
            // Given
            when(restTemplate.getForObject(anyString(), eq(UserResponseDTO.class), any(Object[].class)))
                    .thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> reviewQueryService.getReviewById(reviewId, currentUser))
                    .isInstanceOf(ReviewException.class)
                    .hasFieldOrPropertyWithValue("code", ReviewErrorCode.UNAUTHORIZED_ACCESS);
            
            verify(restTemplate).getForObject(anyString(), eq(UserResponseDTO.class), any(Object[].class));
            verify(reviewRepository, never()).findById(any());
        }

        @Test
        @DisplayName("리뷰를 찾을 수 없으면 예외를 던진다")
        void getReviewById_ReviewNotFound_ThrowsException() {
            // Given
            when(restTemplate.getForObject(anyString(), eq(UserResponseDTO.class), any(Object[].class)))
                    .thenReturn(userResponseDTO);
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> reviewQueryService.getReviewById(reviewId, currentUser))
                    .isInstanceOf(ReviewException.class)
                    .hasFieldOrPropertyWithValue("code", ReviewErrorCode.NOT_FOUND);
            
            verify(restTemplate).getForObject(anyString(), eq(UserResponseDTO.class), any(Object[].class));
            verify(reviewRepository).findById(reviewId);
        }
    }

    @Nested
    @DisplayName("getReviewListByStore 메소드는")
    class GetReviewListByStoreTests {

        @Test
        @DisplayName("유효한 요청이 주어지면 가게의 리뷰 목록을 반환한다")
        void getReviewListByStore_ValidRequest_ReturnsReviewList() {
            // Given
            LocalDateTime cursor = now;
            int size = 10;
            List<ReviewDocument> reviewDocuments = List.of(reviewDocument);
            Slice<ReviewDocument> reviewSlice = new SliceImpl<>(reviewDocuments, Pageable.ofSize(size), false);
            List<ReviewResponseDTO.ReviewStoreResponseDTO> reviewStoreResponseDTOs = List.of(reviewStoreResponseDTO);
            
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
            when(reviewRepository.findAllByStoreId(eq(storeId), eq(cursor), any(Pageable.class)))
                    .thenReturn(reviewSlice);
            
            try (MockedStatic<ReviewConverter> mockedStatic = mockStatic(ReviewConverter.class)) {
                mockedStatic.when(() -> ReviewConverter.toReviewStoreResponseDTO(reviewDocument))
                        .thenReturn(reviewStoreResponseDTO);
                mockedStatic.when(() -> ReviewConverter.toReviewStoreListResponseDTO(eq(reviewStoreResponseDTOs), eq(false), isNull()))
                        .thenReturn(reviewStoreListResponseDTO);

                // When
                ReviewResponseDTO.ReviewStoreListResponseDTO result = 
                        reviewQueryService.getReviewListByStore(storeId, cursor, size, currentUser);

                // Then
                assertThat(result).isEqualTo(reviewStoreListResponseDTO);
                verify(storeRepository).findById(storeId);
                verify(reviewRepository).findAllByStoreId(eq(storeId), eq(cursor), any(Pageable.class));
            }
        }

        @Test
        @DisplayName("cursor가 null이면 기본값을 사용한다")
        void getReviewListByStore_NullCursor_UsesDefaultCursor() {
            // Given
            LocalDateTime nullCursor = null;
            int size = 10;
            List<ReviewDocument> reviewDocuments = List.of(reviewDocument);
            Slice<ReviewDocument> reviewSlice = new SliceImpl<>(reviewDocuments, Pageable.ofSize(size), false);
            List<ReviewResponseDTO.ReviewStoreResponseDTO> reviewStoreResponseDTOs = List.of(reviewStoreResponseDTO);
            
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
            when(reviewRepository.findAllByStoreId(eq(storeId), any(LocalDateTime.class), any(Pageable.class)))
                    .thenReturn(reviewSlice);
            
            try (MockedStatic<ReviewConverter> mockedStatic = mockStatic(ReviewConverter.class)) {
                mockedStatic.when(() -> ReviewConverter.toReviewStoreResponseDTO(reviewDocument))
                        .thenReturn(reviewStoreResponseDTO);
                mockedStatic.when(() -> ReviewConverter.toReviewStoreListResponseDTO(eq(reviewStoreResponseDTOs), eq(false), isNull()))
                        .thenReturn(reviewStoreListResponseDTO);

                // When
                ReviewResponseDTO.ReviewStoreListResponseDTO result = 
                        reviewQueryService.getReviewListByStore(storeId, nullCursor, size, currentUser);

                // Then
                assertThat(result).isEqualTo(reviewStoreListResponseDTO);
                verify(storeRepository).findById(storeId);
                verify(reviewRepository).findAllByStoreId(eq(storeId), any(LocalDateTime.class), any(Pageable.class));
            }
        }

        @Test
        @DisplayName("가게를 찾을 수 없으면 예외를 던진다")
        void getReviewListByStore_StoreNotFound_ThrowsException() {
            // Given
            LocalDateTime cursor = now;
            int size = 10;
            
            when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> reviewQueryService.getReviewListByStore(storeId, cursor, size, currentUser))
                    .isInstanceOf(StoreException.class)
                    .hasFieldOrPropertyWithValue("code", StoreErrorCode.NOT_FOUND);
            
            verify(storeRepository).findById(storeId);
            verify(reviewRepository, never()).findAllByStoreId(any(), any(), any());
        }

        @Test
        @DisplayName("사용자가 null이면 예외를 던진다")
        void getReviewListByStore_NullUser_ThrowsException() {
            // Given
            LocalDateTime cursor = now;
            int size = 10;
            CurrentUser nullUser = null;
            
            // ✅ Store 조회를 성공시켜서 user null 체크까지 도달하게 함
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

            // When & Then
            assertThatThrownBy(() -> reviewQueryService.getReviewListByStore(storeId, cursor, size, nullUser))
                    .isInstanceOf(ReviewException.class)
                    .hasFieldOrPropertyWithValue("code", ReviewErrorCode.UNAUTHORIZED_ACCESS);
            
            verify(storeRepository).findById(storeId);
            verify(reviewRepository, never()).findAllByStoreId(any(), any(), any());
        }

        @Test
        @DisplayName("리뷰가 없으면 예외를 던진다")
        void getReviewListByStore_NoReviews_ThrowsException() {
            // Given
            LocalDateTime cursor = now;
            int size = 10;
            Slice<ReviewDocument> emptySlice = new SliceImpl<>(new ArrayList<>(), Pageable.ofSize(size), false);
            
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
            when(reviewRepository.findAllByStoreId(eq(storeId), eq(cursor), any(Pageable.class)))
                    .thenReturn(emptySlice);

            // When & Then
            assertThatThrownBy(() -> reviewQueryService.getReviewListByStore(storeId, cursor, size, currentUser))
                    .isInstanceOf(ReviewException.class)
                    .hasFieldOrPropertyWithValue("code", ReviewErrorCode.NOT_FOUND);
            
            verify(storeRepository).findById(storeId);
            verify(reviewRepository).findAllByStoreId(eq(storeId), eq(cursor), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("getReviewListByUser 메소드는")
    class GetReviewListByUserTests {

        @Test
        @DisplayName("유효한 요청이 주어지면 사용자의 리뷰 목록을 반환한다")
        void getReviewListByUser_ValidRequest_ReturnsReviewList() {
            // Given
            LocalDateTime cursor = now;
            int size = 10;
            List<ReviewDocument> reviewDocuments = List.of(reviewDocument);
            Slice<ReviewDocument> reviewSlice = new SliceImpl<>(reviewDocuments, Pageable.ofSize(size), false);
            List<ReviewResponseDTO.ReviewUserResponseDTO> reviewUserResponseDTOs = List.of(reviewUserResponseDTO);
            
            when(reviewRepository.findAllByUserId(eq(userId), eq(cursor), any(Pageable.class)))
                    .thenReturn(reviewSlice);
            
            try (MockedStatic<ReviewConverter> mockedStatic = mockStatic(ReviewConverter.class)) {
                mockedStatic.when(() -> ReviewConverter.toReviewUserResponseDTO(reviewDocument))
                        .thenReturn(reviewUserResponseDTO);
                mockedStatic.when(() -> ReviewConverter.toReviewUserListResponseDTO(eq(reviewUserResponseDTOs), eq(false), isNull()))
                        .thenReturn(reviewUserListResponseDTO);

                // When
                ReviewResponseDTO.ReviewUserListResponseDTO result = 
                        reviewQueryService.getReviewListByUser(cursor, size, currentUser);

                // Then
                assertThat(result).isEqualTo(reviewUserListResponseDTO);
                verify(reviewRepository).findAllByUserId(eq(userId), eq(cursor), any(Pageable.class));
            }
        }

        @Test
        @DisplayName("cursor가 null이면 기본값을 사용한다")
        void getReviewListByUser_NullCursor_UsesDefaultCursor() {
            // Given
            LocalDateTime nullCursor = null;
            int size = 10;
            List<ReviewDocument> reviewDocuments = List.of(reviewDocument);
            Slice<ReviewDocument> reviewSlice = new SliceImpl<>(reviewDocuments, Pageable.ofSize(size), false);
            List<ReviewResponseDTO.ReviewUserResponseDTO> reviewUserResponseDTOs = List.of(reviewUserResponseDTO);
            
            when(reviewRepository.findAllByUserId(eq(userId), any(LocalDateTime.class), any(Pageable.class)))
                    .thenReturn(reviewSlice);
            
            try (MockedStatic<ReviewConverter> mockedStatic = mockStatic(ReviewConverter.class)) {
                mockedStatic.when(() -> ReviewConverter.toReviewUserResponseDTO(reviewDocument))
                        .thenReturn(reviewUserResponseDTO);
                mockedStatic.when(() -> ReviewConverter.toReviewUserListResponseDTO(eq(reviewUserResponseDTOs), eq(false), isNull()))
                        .thenReturn(reviewUserListResponseDTO);

                // When
                ReviewResponseDTO.ReviewUserListResponseDTO result = 
                        reviewQueryService.getReviewListByUser(nullCursor, size, currentUser);

                // Then
                assertThat(result).isEqualTo(reviewUserListResponseDTO);
                verify(reviewRepository).findAllByUserId(eq(userId), any(LocalDateTime.class), any(Pageable.class));
            }
        }

        @Test
        @DisplayName("사용자가 null이면 예외를 던진다")
        void getReviewListByUser_NullUser_ThrowsException() {
            // Given
            LocalDateTime cursor = now;
            int size = 10;
            CurrentUser nullUser = null;

            // When & Then
            assertThatThrownBy(() -> reviewQueryService.getReviewListByUser(cursor, size, nullUser))
                    .isInstanceOf(ReviewException.class)
                    .hasFieldOrPropertyWithValue("code", ReviewErrorCode.UNAUTHORIZED_ACCESS);
            
            verify(reviewRepository, never()).findAllByUserId(any(), any(), any());
        }

        @Test
        @DisplayName("리뷰가 없으면 예외를 던진다")
        void getReviewListByUser_NoReviews_ThrowsException() {
            // Given
            LocalDateTime cursor = now;
            int size = 10;
            Slice<ReviewDocument> emptySlice = new SliceImpl<>(new ArrayList<>(), Pageable.ofSize(size), false);
            
            when(reviewRepository.findAllByUserId(eq(userId), eq(cursor), any(Pageable.class)))
                    .thenReturn(emptySlice);

            // When & Then
            assertThatThrownBy(() -> reviewQueryService.getReviewListByUser(cursor, size, currentUser))
                    .isInstanceOf(ReviewException.class)
                    .hasFieldOrPropertyWithValue("code", ReviewErrorCode.NOT_FOUND);
            
            verify(reviewRepository).findAllByUserId(eq(userId), eq(cursor), any(Pageable.class));
        }
    }
}