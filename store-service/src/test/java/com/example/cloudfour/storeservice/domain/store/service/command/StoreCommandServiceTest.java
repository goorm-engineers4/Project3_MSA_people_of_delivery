package com.example.cloudfour.storeservice.domain.store.service.command;

import com.example.cloudfour.modulecommon.dto.CurrentUser;
import com.example.cloudfour.storeservice.domain.region.entity.Region;
import com.example.cloudfour.storeservice.domain.region.exception.RegionErrorCode;
import com.example.cloudfour.storeservice.domain.region.exception.RegionException;
import com.example.cloudfour.storeservice.domain.region.repository.RegionRepository;
import com.example.cloudfour.storeservice.domain.region.service.RegionService;
import com.example.cloudfour.storeservice.domain.store.controller.StoreCommonRequestDTO;
import com.example.cloudfour.storeservice.domain.store.converter.StoreConverter;
import com.example.cloudfour.storeservice.domain.store.dto.StoreRequestDTO;
import com.example.cloudfour.storeservice.domain.store.dto.StoreResponseDTO;
import com.example.cloudfour.storeservice.domain.store.entity.Store;
import com.example.cloudfour.storeservice.domain.store.entity.StoreCategory;
import com.example.cloudfour.storeservice.domain.store.exception.StoreErrorCode;
import com.example.cloudfour.storeservice.domain.store.exception.StoreException;
import com.example.cloudfour.storeservice.domain.store.repository.StoreCategoryRepository;
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
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoreCommandService 단위테스트")
class StoreCommandServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private RegionService regionService;

    @Mock
    private StoreCategoryRepository storeCategoryRepository;

    @InjectMocks
    private StoreCommandService storeCommandService;

    private UUID userId;
    private UUID storeId;
    private UUID regionId;
    private CurrentUser currentUser;
    private StoreRequestDTO.StoreCreateRequestDTO createRequestDTO;
    private StoreRequestDTO.StoreUpdateRequestDTO updateRequestDTO;
    private Store store;
    private StoreCategory storeCategory;
    private Region region;
    private StoreResponseDTO.StoreCreateResponseDTO createResponseDTO;
    private StoreResponseDTO.StoreUpdateResponseDTO updateResponseDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        regionId = UUID.randomUUID();
        currentUser = new CurrentUser(userId, "ROLE_OWNER");

        // Set up StoreCommonRequestDTO
        StoreCommonRequestDTO storeCommonRequestDTO = StoreCommonRequestDTO.builder()
                .name("Test Store")
                .address("Test Address")
                .category("Test Category")
                .build();

        // Set up StoreCreateRequestDTO
        createRequestDTO = StoreRequestDTO.StoreCreateRequestDTO.builder()
                .storeCommonRequestDTO(storeCommonRequestDTO)
                .storePicture("test.jpg")
                .phone("123-456-7890")
                .content("Test Content")
                .minPrice(10000)
                .deliveryTip(2000)
                .operationHours("9:00-18:00")
                .closedDays("Sunday")
                .build();

        // Set up StoreUpdateRequestDTO
        updateRequestDTO = StoreRequestDTO.StoreUpdateRequestDTO.builder()
                .storeCommonRequestDTO(storeCommonRequestDTO)
                .build();

        // Set up StoreCategory
        storeCategory = mock(StoreCategory.class);
        lenient().when(storeCategory.getCategory()).thenReturn("Test Category");

        // Set up Region
        region = mock(Region.class);
        lenient().when(region.getId()).thenReturn(regionId);

        // Set up Store
        store = mock(Store.class);
        lenient().when(store.getId()).thenReturn(storeId);
        lenient().when(store.getName()).thenReturn("Test Store");
        lenient().when(store.getAddress()).thenReturn("Test Address");
        lenient().when(store.getOwnerId()).thenReturn(userId);
        lenient().when(store.getStoreCategory()).thenReturn(storeCategory);
        lenient().when(store.getCreatedAt()).thenReturn(LocalDateTime.now());
        lenient().when(store.getUpdatedAt()).thenReturn(LocalDateTime.now());

        // Set up response DTOs
        createResponseDTO = mock(StoreResponseDTO.StoreCreateResponseDTO.class);
        updateResponseDTO = mock(StoreResponseDTO.StoreUpdateResponseDTO.class);
    }

    @Nested
    @DisplayName("createStore 메소드는")
    class CreateStoreTests {

        @Test
        @DisplayName("유효한 요청이 주어지면 가게를 생성하고 응답을 반환한다")
        void createStore_ValidRequest_ReturnsResponse() {
            // Given
            String storeName = createRequestDTO.getStoreCommonRequestDTO().getName();
            String categoryName = createRequestDTO.getStoreCommonRequestDTO().getCategory();
            String storeAddress = createRequestDTO.getStoreCommonRequestDTO().getAddress();
            
            when(storeRepository.existsByNameAndIsDeletedFalse(storeName)).thenReturn(false);
            when(storeCategoryRepository.findByCategory(categoryName)).thenReturn(Optional.of(storeCategory));
            when(regionService.parseAndSaveRegion(storeAddress)).thenReturn(regionId);
            when(regionRepository.findById(regionId)).thenReturn(Optional.of(region));
            
            try (MockedStatic<StoreConverter> mockedStatic = mockStatic(StoreConverter.class)) {
                mockedStatic.when(() -> StoreConverter.toStore(createRequestDTO)).thenReturn(store);
                mockedStatic.when(() -> StoreConverter.toStoreCreateResponseDTO(store)).thenReturn(createResponseDTO);

                // When
                StoreResponseDTO.StoreCreateResponseDTO result = storeCommandService.createStore(createRequestDTO, currentUser);

                // Then
                assertThat(result).isEqualTo(createResponseDTO);
                verify(storeRepository).existsByNameAndIsDeletedFalse(storeName);
                verify(storeCategoryRepository).findByCategory(categoryName);
                verify(regionService).parseAndSaveRegion(storeAddress);
                verify(regionRepository).findById(regionId);
                verify(store).setStoreCategory(storeCategory);
                verify(store).setRegion(region);
                verify(store).setOwnerId(userId);
                verify(storeRepository).save(store);
            }
        }

        @Test
        @DisplayName("사용자가 null이면 예외를 던진다")
        void createStore_NullUser_ThrowsException() {
            // Given
            CurrentUser nullUser = null;

            // When & Then
            StoreException exception = org.junit.jupiter.api.Assertions.assertThrows(
                StoreException.class,
                () -> storeCommandService.createStore(createRequestDTO, nullUser)
            );
            
            org.junit.jupiter.api.Assertions.assertEquals(StoreErrorCode.UNAUTHORIZED_ACCESS, exception.getCode());
        }

        @Test
        @DisplayName("이미 존재하는 가게 이름이면 예외를 던진다")
        void createStore_DuplicateStoreName_ThrowsException() {
            // Given
            String storeName = createRequestDTO.getStoreCommonRequestDTO().getName();
            when(storeRepository.existsByNameAndIsDeletedFalse(storeName)).thenReturn(true);

            // When & Then
            StoreException exception = org.junit.jupiter.api.Assertions.assertThrows(
                StoreException.class,
                () -> storeCommandService.createStore(createRequestDTO, currentUser)
            );
            
            org.junit.jupiter.api.Assertions.assertEquals(StoreErrorCode.ALREADY_ADD, exception.getCode());
        }

        @Test
        @DisplayName("지역을 찾을 수 없으면 예외를 던진다")
        void createStore_RegionNotFound_ThrowsException() {
            // Given
            String storeName = createRequestDTO.getStoreCommonRequestDTO().getName();
            String categoryName = createRequestDTO.getStoreCommonRequestDTO().getCategory();
            String storeAddress = createRequestDTO.getStoreCommonRequestDTO().getAddress();
            
            when(storeRepository.existsByNameAndIsDeletedFalse(storeName)).thenReturn(false);
            when(storeCategoryRepository.findByCategory(categoryName)).thenReturn(Optional.of(storeCategory));
            when(regionService.parseAndSaveRegion(storeAddress)).thenReturn(regionId);
            when(regionRepository.findById(regionId)).thenReturn(Optional.empty());

            try (MockedStatic<StoreConverter> mockedStatic = mockStatic(StoreConverter.class)) {
                mockedStatic.when(() -> StoreConverter.toStore(createRequestDTO)).thenReturn(store);

                // When & Then
                RegionException exception = org.junit.jupiter.api.Assertions.assertThrows(
                    RegionException.class,
                    () -> storeCommandService.createStore(createRequestDTO, currentUser)
                );
                
                org.junit.jupiter.api.Assertions.assertEquals(RegionErrorCode.NOT_FOUND, exception.getCode());
            }
        }
    }

    @Nested
    @DisplayName("updateStore 메소드는")
    class UpdateStoreTests {

        @Test
        @DisplayName("유효한 요청이 주어지면 가게를 수정하고 응답을 반환한다")
        void updateStore_ValidRequest_ReturnsResponse() {
            // Given
            String storeName = updateRequestDTO.getStoreCommonRequestDTO().getName();
            String categoryName = updateRequestDTO.getStoreCommonRequestDTO().getCategory();
            String storeAddress = updateRequestDTO.getStoreCommonRequestDTO().getAddress();
            
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
            when(storeRepository.existsByNameAndIsDeletedFalse(storeName)).thenReturn(false);
            when(storeCategoryRepository.findByCategory(categoryName)).thenReturn(Optional.of(storeCategory));
            
            try (MockedStatic<StoreConverter> mockedStatic = mockStatic(StoreConverter.class)) {
                mockedStatic.when(() -> StoreConverter.toStoreUpdateResponseDTO(store)).thenReturn(updateResponseDTO);

                // When
                StoreResponseDTO.StoreUpdateResponseDTO result = storeCommandService.updateStore(storeId, updateRequestDTO, currentUser);

                // Then
                assertThat(result).isEqualTo(updateResponseDTO);
                verify(storeRepository).findById(storeId);
                verify(store).setStoreCategory(storeCategory);
                verify(store).update(storeName, storeAddress);
                verify(storeRepository).save(store);
            }
        }

        @Test
        @DisplayName("가게를 찾을 수 없으면 예외를 던진다")
        void updateStore_StoreNotFound_ThrowsException() {
            // Given
            when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

            // When & Then
            StoreException exception = org.junit.jupiter.api.Assertions.assertThrows(
                StoreException.class,
                () -> storeCommandService.updateStore(storeId, updateRequestDTO, currentUser)
            );
            
            org.junit.jupiter.api.Assertions.assertEquals(StoreErrorCode.NOT_FOUND, exception.getCode());
        }

        @Test
        @DisplayName("사용자가 null이면 예외를 던진다")
        void updateStore_NullUser_ThrowsException() {
            // Given
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
            CurrentUser nullUser = null;

            // When & Then
            StoreException exception = org.junit.jupiter.api.Assertions.assertThrows(
                StoreException.class,
                () -> storeCommandService.updateStore(storeId, updateRequestDTO, nullUser)
            );
            
            org.junit.jupiter.api.Assertions.assertEquals(StoreErrorCode.UNAUTHORIZED_ACCESS, exception.getCode());
        }

        @Test
        @DisplayName("사용자가 가게 소유자가 아니면 예외를 던진다")
        void updateStore_UnauthorizedUser_ThrowsException() {
            // Given
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
            CurrentUser unauthorizedUser = new CurrentUser(UUID.randomUUID(), "ROLE_OWNER");

            // When & Then
            StoreException exception = org.junit.jupiter.api.Assertions.assertThrows(
                StoreException.class,
                () -> storeCommandService.updateStore(storeId, updateRequestDTO, unauthorizedUser)
            );
            
            org.junit.jupiter.api.Assertions.assertEquals(StoreErrorCode.UNAUTHORIZED_ACCESS, exception.getCode());
        }

        @Test
        @DisplayName("이미 존재하는 가게 이름이면 예외를 던진다")
        void updateStore_DuplicateStoreName_ThrowsException() {
            // Given
            String storeName = updateRequestDTO.getStoreCommonRequestDTO().getName();
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
            when(storeRepository.existsByNameAndIsDeletedFalse(storeName)).thenReturn(true);

            // When & Then
            StoreException exception = org.junit.jupiter.api.Assertions.assertThrows(
                StoreException.class,
                () -> storeCommandService.updateStore(storeId, updateRequestDTO, currentUser)
            );
            
            org.junit.jupiter.api.Assertions.assertEquals(StoreErrorCode.ALREADY_ADD, exception.getCode());
        }
    }

    @Nested
    @DisplayName("deleteStore 메소드는")
    class DeleteStoreTests {

        @Test
        @DisplayName("유효한 요청이 주어지면 가게를 삭제한다")
        void deleteStore_ValidRequest_DeletesStore() {
            // Given
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

            // When
            storeCommandService.deleteStore(storeId, currentUser);

            // Then
            verify(storeRepository).findById(storeId);
            verify(store).softDelete();
            verify(storeRepository).save(store);
        }

        @Test
        @DisplayName("가게를 찾을 수 없으면 예외를 던진다")
        void deleteStore_StoreNotFound_ThrowsException() {
            // Given
            when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

            // When & Then
            StoreException exception = org.junit.jupiter.api.Assertions.assertThrows(
                StoreException.class,
                () -> storeCommandService.deleteStore(storeId, currentUser)
            );
            
            org.junit.jupiter.api.Assertions.assertEquals(StoreErrorCode.NOT_FOUND, exception.getCode());
        }

        @Test
        @DisplayName("사용자가 null이면 예외를 던진다")
        void deleteStore_NullUser_ThrowsException() {
            // Given
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
            CurrentUser nullUser = null;

            // When & Then
            StoreException exception = org.junit.jupiter.api.Assertions.assertThrows(
                StoreException.class,
                () -> storeCommandService.deleteStore(storeId, nullUser)
            );
            
            org.junit.jupiter.api.Assertions.assertEquals(StoreErrorCode.UNAUTHORIZED_ACCESS, exception.getCode());
        }

        @Test
        @DisplayName("사용자가 가게 소유자가 아니면 예외를 던진다")
        void deleteStore_UnauthorizedUser_ThrowsException() {
            // Given
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
            CurrentUser unauthorizedUser = new CurrentUser(UUID.randomUUID(), "ROLE_OWNER");

            // When & Then
            StoreException exception = org.junit.jupiter.api.Assertions.assertThrows(
                StoreException.class,
                () -> storeCommandService.deleteStore(storeId, unauthorizedUser)
            );
            
            org.junit.jupiter.api.Assertions.assertEquals(StoreErrorCode.UNAUTHORIZED_ACCESS, exception.getCode());
        }
    }
}