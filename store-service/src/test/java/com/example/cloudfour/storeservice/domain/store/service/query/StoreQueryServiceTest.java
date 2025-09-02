package com.example.cloudfour.storeservice.domain.store.service.query;

import com.example.cloudfour.modulecommon.dto.CurrentUser;
import com.example.cloudfour.storeservice.domain.collection.document.StoreDocument;
import com.example.cloudfour.storeservice.domain.collection.repository.query.StoreSearchRepository;
import com.example.cloudfour.storeservice.domain.common.RegionResponseDTO;
import com.example.cloudfour.storeservice.domain.commondto.StoreCartResponseDTO;
import com.example.cloudfour.storeservice.domain.region.exception.RegionErrorCode;
import com.example.cloudfour.storeservice.domain.region.exception.RegionException;
import com.example.cloudfour.storeservice.domain.store.converter.StoreConverter;
import com.example.cloudfour.storeservice.domain.store.dto.StoreResponseDTO;
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
@DisplayName("StoreQueryService 단위테스트")
class StoreQueryServiceTest {

    @Mock
    private StoreSearchRepository storeSearchRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private StoreQueryService storeQueryService;

    private UUID storeId;
    private UUID categoryId;
    private UUID userId;
    private CurrentUser currentUser;
    private Store store;
    private StoreDocument storeDocument;
    private RegionResponseDTO regionResponseDTO;
    private LocalDateTime now;
    private List<StoreDocument> storeDocuments;
    private Slice<StoreDocument> storeDocumentSlice;
    private StoreResponseDTO.StoreCursorListResponseDTO storeCursorListResponseDTO;
    private StoreResponseDTO.StoreDetailResponseDTO storeDetailResponseDTO;
    private StoreCartResponseDTO storeCartResponseDTO;
    private StoreResponseDTO.StoreListResponseDTO storeListResponseDTO;

    @BeforeEach
    void setUp() {
        // Initialize test data
        storeId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        userId = UUID.randomUUID();
        currentUser = new CurrentUser(userId, "testUser");
        now = LocalDateTime.now();

        // Mock store
        store = mock(Store.class);
        lenient().when(store.getId()).thenReturn(storeId);
        lenient().when(store.getName()).thenReturn("Test Store");
        lenient().when(store.getOwnerId()).thenReturn(userId);

        // Mock store document
        storeDocument = mock(StoreDocument.class);
        lenient().when(storeDocument.getId()).thenReturn(storeId.toString());
        lenient().when(storeDocument.getStoreId()).thenReturn(storeId);
        lenient().when(storeDocument.getUserId()).thenReturn(userId);
        lenient().when(storeDocument.getName()).thenReturn("Test Store");
        lenient().when(storeDocument.getAddress()).thenReturn("Test Address");
        lenient().when(storeDocument.getPhone()).thenReturn("123-456-7890");
        lenient().when(storeDocument.getContent()).thenReturn("Test Content");
        lenient().when(storeDocument.getMinPrice()).thenReturn(10000);
        lenient().when(storeDocument.getDeliveryTip()).thenReturn(2000);
        lenient().when(storeDocument.getRating()).thenReturn(4.5f);
        lenient().when(storeDocument.getLikeCount()).thenReturn(100);
        lenient().when(storeDocument.getReviewCount()).thenReturn(50);
        lenient().when(storeDocument.getSiDo()).thenReturn("Test SiDo");
        lenient().when(storeDocument.getSiGunGu()).thenReturn("Test SiGunGu");
        lenient().when(storeDocument.getEupMyeonDong()).thenReturn("Test EupMyeonDong");
        lenient().when(storeDocument.getCreatedAt()).thenReturn(now);

        // Mock region response
        regionResponseDTO = mock(RegionResponseDTO.class);
        lenient().when(regionResponseDTO.getSiDo()).thenReturn("Test SiDo");
        lenient().when(regionResponseDTO.getSiGunGu()).thenReturn("Test SiGunGu");
        lenient().when(regionResponseDTO.getEupMyeonDong()).thenReturn("Test EupMyeonDong");

        // Mock response DTOs - 실제 인스턴스 생성하거나 적절한 mock 설정
        storeCursorListResponseDTO = mock(StoreResponseDTO.StoreCursorListResponseDTO.class);
        lenient().when(storeCursorListResponseDTO.getStoreList()).thenReturn(new ArrayList<>());
        lenient().when(storeCursorListResponseDTO.getNextCursor()).thenReturn(now);
        
        storeDetailResponseDTO = mock(StoreResponseDTO.StoreDetailResponseDTO.class);
        
        storeCartResponseDTO = mock(StoreCartResponseDTO.class);
        lenient().when(storeCartResponseDTO.getStoreId()).thenReturn(storeId);
        lenient().when(storeCartResponseDTO.getUserId()).thenReturn(userId);
        lenient().when(storeCartResponseDTO.getName()).thenReturn("Test Store");
        
        storeListResponseDTO = mock(StoreResponseDTO.StoreListResponseDTO.class);
        lenient().when(storeListResponseDTO.getCreatedAt()).thenReturn(now);

        // Create list of store documents
        storeDocuments = new ArrayList<>();
        storeDocuments.add(storeDocument);

        // Mock slice of store documents
        storeDocumentSlice = mock(Slice.class);
        lenient().when(storeDocumentSlice.getContent()).thenReturn(storeDocuments);
        lenient().when(storeDocumentSlice.hasNext()).thenReturn(false);
        lenient().when(storeDocumentSlice.isEmpty()).thenReturn(false);
    }

    @Nested
    @DisplayName("getAllStores 메소드는")
    class GetAllStoresTests {

        @Test
        @DisplayName("유효한 요청이 주어지면 가게 목록을 반환한다")
        void getAllStores_ValidRequest_ReturnsStores() {
            // Given
            LocalDateTime cursor = now;
            int size = 10;
            String keyword = "Test";

            // RestTemplate mock 설정
            when(restTemplate.getForObject(anyString(), eq(RegionResponseDTO.class), any(Object[].class)))
                    .thenReturn(regionResponseDTO);
            
            // Repository mock 설정 - 모든 매개변수를 matcher로 통일
            when(storeSearchRepository.findAllStoreByKeyWordAndRegion(
                    eq(keyword), eq(cursor), any(Pageable.class),
                    anyString(), anyString(), anyString()))
                    .thenReturn(storeDocumentSlice);

            // Static mock 설정
            try (MockedStatic<StoreConverter> mockedStatic = mockStatic(StoreConverter.class)) {
                mockedStatic.when(() -> StoreConverter.toStoreListResponseDTO(any(StoreDocument.class)))
                        .thenReturn(storeListResponseDTO);
                mockedStatic.when(() -> StoreConverter.toStoreCursorListResponseDTO(anyList(), any()))
                        .thenReturn(storeCursorListResponseDTO);

                // When
                StoreResponseDTO.StoreCursorListResponseDTO result = 
                    storeQueryService.getAllStores(cursor, size, keyword, currentUser);

                // Then
                assertThat(result).isEqualTo(storeCursorListResponseDTO);
                
                // Verification - 모든 매개변수를 matcher로 통일
                verify(restTemplate).getForObject(anyString(), eq(RegionResponseDTO.class), any(Object[].class));
                verify(storeSearchRepository).findAllStoreByKeyWordAndRegion(
                        eq(keyword), eq(cursor), any(Pageable.class),
                        anyString(), anyString(), anyString());
                
                // Static method 호출 검증
                mockedStatic.verify(() -> StoreConverter.toStoreListResponseDTO(any(StoreDocument.class)));
                mockedStatic.verify(() -> StoreConverter.toStoreCursorListResponseDTO(anyList(), any()));
            }
        }

        @Test
        @DisplayName("사용자가 null이면 예외를 던진다")
        void getAllStores_NullUser_ThrowsException() {
            // Given
            LocalDateTime cursor = now;
            int size = 10;
            String keyword = "Test";
            CurrentUser nullUser = null;

            // When & Then
            assertThatThrownBy(() -> storeQueryService.getAllStores(cursor, size, keyword, nullUser))
                    .isInstanceOf(StoreException.class)
                    .hasFieldOrPropertyWithValue("code", StoreErrorCode.UNAUTHORIZED_ACCESS);
            
            // ✅ 구체적인 매개변수 타입 지정
            verify(restTemplate, never()).getForObject(anyString(), any(Class.class), any(Object[].class));
            verify(storeSearchRepository, never()).findAllStoreByKeyWordAndRegion(
                    any(), any(), any(), any(), any(), any());

        }

        @Test
        @DisplayName("지역이 존재하지 않으면 예외를 던진다")
        void getAllStores_RegionNotFound_ThrowsException() {
            // Given
            LocalDateTime cursor = now;
            int size = 10;
            String keyword = "Test";

            when(restTemplate.getForObject(anyString(), eq(RegionResponseDTO.class), any(Object[].class)))
                    .thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> storeQueryService.getAllStores(cursor, size, keyword, currentUser))
                    .isInstanceOf(RegionException.class)
                    .hasFieldOrPropertyWithValue("code", RegionErrorCode.NOT_FOUND);
            
            verify(restTemplate).getForObject(anyString(), eq(RegionResponseDTO.class), any(Object[].class));
            verify(storeSearchRepository, never()).findAllStoreByKeyWordAndRegion(
                    any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("커서가 null이면 현재 시간을 사용한다")
        void getAllStores_NullCursor_UsesCurrentTime() {
            // Given
            LocalDateTime cursor = null;
            int size = 10;
            String keyword = "Test";

            when(restTemplate.getForObject(anyString(), eq(RegionResponseDTO.class), any(Object[].class)))
                    .thenReturn(regionResponseDTO);
            when(storeSearchRepository.findAllStoreByKeyWordAndRegion(
                    eq(keyword), any(LocalDateTime.class), any(Pageable.class),
                    anyString(), anyString(), anyString()))
                    .thenReturn(storeDocumentSlice);

            try (MockedStatic<StoreConverter> mockedStatic = mockStatic(StoreConverter.class)) {
                mockedStatic.when(() -> StoreConverter.toStoreListResponseDTO(any(StoreDocument.class)))
                        .thenReturn(storeListResponseDTO);
                mockedStatic.when(() -> StoreConverter.toStoreCursorListResponseDTO(anyList(), any()))
                        .thenReturn(storeCursorListResponseDTO);

                // When
                StoreResponseDTO.StoreCursorListResponseDTO result = 
                    storeQueryService.getAllStores(cursor, size, keyword, currentUser);

                // Then
                assertThat(result).isEqualTo(storeCursorListResponseDTO);
                verify(restTemplate).getForObject(anyString(), eq(RegionResponseDTO.class), any(Object[].class));
                verify(storeSearchRepository).findAllStoreByKeyWordAndRegion(
                        eq(keyword), any(LocalDateTime.class), any(Pageable.class),
                        anyString(), anyString(), anyString());
            }
        }
    }

    @Nested
    @DisplayName("getStoresByCategory 메소드는")
    class GetStoresByCategoryTests {

        @Test
        @DisplayName("유효한 요청이 주어지면 카테고리별 가게 목록을 반환한다")
        void getStoresByCategory_ValidRequest_ReturnsStores() {
            // Given
            LocalDateTime cursor = now;
            int size = 10;

            when(restTemplate.getForObject(anyString(), eq(RegionResponseDTO.class), any(Object[].class)))
                    .thenReturn(regionResponseDTO);

            when(storeSearchRepository.findAllStoreByCategoryAndCursor(
                    eq(categoryId), eq(cursor), any(Pageable.class),
                    anyString(), anyString(), anyString()))
                    .thenReturn(storeDocumentSlice);

            try (MockedStatic<StoreConverter> converterMock = mockStatic(StoreConverter.class);
                 MockedStatic<StoreResponseDTO.StoreCursorListResponseDTO> responseMock = 
                         mockStatic(StoreResponseDTO.StoreCursorListResponseDTO.class)) {
        
                // ✅ Static method stubbing 올바른 방식
                converterMock.when(() -> StoreConverter.toStoreListResponseDTO(storeDocument))
                        .thenReturn(storeListResponseDTO);
        
                // ✅ Static factory method stubbing
                List<StoreResponseDTO.StoreListResponseDTO> expectedList = List.of(storeListResponseDTO);
                responseMock.when(() -> StoreResponseDTO.StoreCursorListResponseDTO.of(expectedList, null))
                        .thenReturn(storeCursorListResponseDTO);

                // When
                StoreResponseDTO.StoreCursorListResponseDTO result =
                        storeQueryService.getStoresByCategory(categoryId, cursor, size, currentUser);

                // Then
                assertThat(result).isEqualTo(storeCursorListResponseDTO);
                verify(restTemplate).getForObject(anyString(), eq(RegionResponseDTO.class), any(Object[].class));
                verify(storeSearchRepository).findAllStoreByCategoryAndCursor(
                        eq(categoryId), eq(cursor), any(Pageable.class),
                        anyString(), anyString(), anyString());
        
                // ✅ Static method verify
                converterMock.verify(() -> StoreConverter.toStoreListResponseDTO(storeDocument));
                responseMock.verify(() -> StoreResponseDTO.StoreCursorListResponseDTO.of(expectedList, null));
            }
        }

        @Test
        @DisplayName("사용자가 null이면 예외를 던진다")
        void getStoresByCategory_NullUser_ThrowsException() {
            // Given
            LocalDateTime cursor = now;
            int size = 10;
            CurrentUser nullUser = null;

            // When & Then
            assertThatThrownBy(() -> storeQueryService.getStoresByCategory(categoryId, cursor, size, nullUser))
                    .isInstanceOf(StoreException.class)
                    .hasFieldOrPropertyWithValue("code", StoreErrorCode.UNAUTHORIZED_ACCESS);
            
            // ✅ 구체적인 매개변수 타입 지정
            verify(restTemplate, never()).getForObject(anyString(), any(Class.class), any(Object[].class));
            verify(storeSearchRepository, never()).findAllStoreByCategoryAndCursor(
                    any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("지역이 존재하지 않으면 예외를 던진다")
        void getStoresByCategory_RegionNotFound_ThrowsException() {
            // Given
            LocalDateTime cursor = now;
            int size = 10;

            when(restTemplate.getForObject(anyString(), eq(RegionResponseDTO.class), any(Object[].class)))
                    .thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> storeQueryService.getStoresByCategory(categoryId, cursor, size, currentUser))
                    .isInstanceOf(RegionException.class)
                    .hasFieldOrPropertyWithValue("code", RegionErrorCode.NOT_FOUND);
            
            verify(restTemplate).getForObject(anyString(), eq(RegionResponseDTO.class), any(Object[].class));
            verify(storeSearchRepository, never()).findAllStoreByCategoryAndCursor(
                    any(), any(), any(), any(), any(), any());
        }

    }

    @Nested
    @DisplayName("getStoreById 메소드는")
    class GetStoreByIdTests {

        @Test
        @DisplayName("유효한 요청이 주어지면 가게 상세 정보를 반환한다")
        void getStoreById_ValidRequest_ReturnsStore() {
            // Given
            when(storeSearchRepository.findStoreByStoreId(eq(storeId)))
                    .thenReturn(Optional.of(storeDocument));

            try (MockedStatic<StoreConverter> mockedStatic = mockStatic(StoreConverter.class)) {
                mockedStatic.when(() -> StoreConverter.documentToStoreDetailResponseDTO(any(StoreDocument.class)))
                        .thenReturn(storeDetailResponseDTO);

                // When
                StoreResponseDTO.StoreDetailResponseDTO result = storeQueryService.getStoreById(storeId, currentUser);

                // Then
                assertThat(result).isEqualTo(storeDetailResponseDTO);
                verify(storeSearchRepository).findStoreByStoreId(eq(storeId));
                mockedStatic.verify(() -> StoreConverter.documentToStoreDetailResponseDTO(any(StoreDocument.class)));
            }
        }

        @Test
        @DisplayName("사용자가 null이면 예외를 던진다")
        void getStoreById_NullUser_ThrowsException() {
            // Given
            CurrentUser nullUser = null;

            // When & Then
            assertThatThrownBy(() -> storeQueryService.getStoreById(storeId, nullUser))
                    .isInstanceOf(StoreException.class)
                    .hasFieldOrPropertyWithValue("code", StoreErrorCode.UNAUTHORIZED_ACCESS);
            
            verify(storeSearchRepository, never()).findStoreByStoreId(any());
        }

        @Test
        @DisplayName("가게가 존재하지 않으면 예외를 던진다")
        void getStoreById_StoreNotFound_ThrowsException() {
            // Given
            when(storeSearchRepository.findStoreByStoreId(eq(storeId)))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> storeQueryService.getStoreById(storeId, currentUser))
                    .isInstanceOf(StoreException.class)
                    .hasFieldOrPropertyWithValue("code", StoreErrorCode.NOT_FOUND);
            
            verify(storeSearchRepository).findStoreByStoreId(eq(storeId));
        }
    }

    @Nested
    @DisplayName("findStore 메소드는")
    class FindStoreTests {

        @Test
        @DisplayName("유효한 요청이 주어지면 가게 정보를 반환한다")
        void findStore_ValidRequest_ReturnsStore() {
            // Given
            when(storeRepository.findByIdAndIsDeletedFalse(eq(storeId)))
                    .thenReturn(Optional.of(store));

            try (MockedStatic<StoreConverter> mockedStatic = mockStatic(StoreConverter.class)) {
                mockedStatic.when(() -> StoreConverter.toFindStoreDTO(any(Store.class)))
                        .thenReturn(storeCartResponseDTO);

                // When
                StoreCartResponseDTO result = storeQueryService.findStore(storeId);

                // Then
                assertThat(result).isEqualTo(storeCartResponseDTO);
                verify(storeRepository).findByIdAndIsDeletedFalse(eq(storeId));
                mockedStatic.verify(() -> StoreConverter.toFindStoreDTO(any(Store.class)));
            }
        }

        @Test
        @DisplayName("가게가 존재하지 않으면 예외를 던진다")
        void findStore_StoreNotFound_ThrowsException() {
            // Given
            when(storeRepository.findByIdAndIsDeletedFalse(eq(storeId)))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> storeQueryService.findStore(storeId))
                    .isInstanceOf(StoreException.class)
                    .hasFieldOrPropertyWithValue("code", StoreErrorCode.NOT_FOUND);
            
            verify(storeRepository).findByIdAndIsDeletedFalse(eq(storeId));
        }
    }
}