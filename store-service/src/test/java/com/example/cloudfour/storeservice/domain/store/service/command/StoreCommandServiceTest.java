package com.example.cloudfour.storeservice.domain.store.service.command;

import com.example.cloudfour.modulecommon.dto.Passport;
import com.example.cloudfour.storeservice.domain.region.entity.Region;
import com.example.cloudfour.storeservice.domain.region.exception.RegionErrorCode;
import com.example.cloudfour.storeservice.domain.region.exception.RegionException;
import com.example.cloudfour.storeservice.domain.region.repository.RegionRepository;
import com.example.cloudfour.storeservice.domain.region.service.RegionService;
import com.example.cloudfour.storeservice.domain.store.controller.StoreCommonRequestDTO;
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
class StoreCommandServiceTest {

    @Mock private StoreRepository storeRepository;
    @Mock private RegionRepository regionRepository;
    @Mock private RegionService regionService;
    @Mock private StoreCategoryRepository storeCategoryRepository;

    @InjectMocks private StoreCommandService storeCommandService;

    private Passport ownerPassport;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        ownerPassport = Passport.builder().userId(ownerId).build();
    }

    private StoreRequestDTO.StoreCreateRequestDTO sampleCreateDTO(String name, String address, String category) {
        StoreCommonRequestDTO common = StoreCommonRequestDTO.builder()
                .name(name)
                .address(address)
                .category(category)
                .build();
        return StoreRequestDTO.StoreCreateRequestDTO.builder()
                .storeCommonRequestDTO(common)
                .storePicture("pic")
                .phone("010-1234-5678")
                .content("content")
                .minPrice(1000)
                .deliveryTip(100)
                .operationHours("09:00-18:00")
                .closedDays("sun")
                .build();
    }

    @Test
    @DisplayName("createStore: 성공적으로 저장 및 응답 반환")
    void createStore_success() {
        // given
        var dto = sampleCreateDTO("store-a", "서울 강남구 역삼동", "KOREAN");

        given(storeRepository.existsByNameAndIsDeletedFalse("store-a")).willReturn(false);

        StoreCategory category = StoreCategory.builder().category("KOREAN").build();
        given(storeCategoryRepository.findByCategory("KOREAN")).willReturn(Optional.empty());
        given(storeCategoryRepository.save(any(StoreCategory.class))).willReturn(category);

        UUID regionId = UUID.randomUUID();
        given(regionService.parseAndSaveRegion("서울 강남구 역삼동")).willReturn(regionId);

        Region region = Region.builder().siDo("서울").siGunGu("강남구").eupMyeonDong("역삼동").build();
        given(regionRepository.findById(regionId)).willReturn(Optional.of(region));

        given(storeRepository.save(any(Store.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        StoreResponseDTO.StoreCreateResponseDTO res = storeCommandService.createStore(dto, ownerPassport);

        // then
        assertThat(res.getStoreCommonsBaseResponseDTO().getName()).isEqualTo("store-a");
        assertThat(res.getStoreCommonMainResponseDTO().getCategory()).isEqualTo("KOREAN");
        assertThat(res.getCreatedBy()).isEqualTo(ownerId);

        ArgumentCaptor<Store> captor = ArgumentCaptor.forClass(Store.class);
        verify(storeRepository).save(captor.capture());
        Store saved = captor.getValue();
        assertThat(saved.getOwnerId()).isEqualTo(ownerId);
        assertThat(saved.getRegion()).isEqualTo(region);
        assertThat(saved.getStoreCategory()).isEqualTo(category);
    }

    @Test
    @DisplayName("createStore: 인증정보 없음 -> UNAUTHORIZED_ACCESS")
    void createStore_unauthorized() {
        var dto = sampleCreateDTO("store-a", "서울 강남구 역삼동", "KOREAN");
        assertThatThrownBy(() -> storeCommandService.createStore(dto, null))
                .isInstanceOf(StoreException.class)
                .hasMessageContaining(StoreErrorCode.UNAUTHORIZED_ACCESS.getMessage());
        verifyNoInteractions(storeRepository, regionRepository, regionService, storeCategoryRepository);
    }

    @Test
    @DisplayName("createStore: 중복 이름 -> ALREADY_ADD")
    void createStore_duplicateName() {
        var dto = sampleCreateDTO("dup-store", "서울 강남구 역삼동", "KOREAN");
        given(storeRepository.existsByNameAndIsDeletedFalse("dup-store")).willReturn(true);
        assertThatThrownBy(() -> storeCommandService.createStore(dto, ownerPassport))
                .isInstanceOf(StoreException.class)
                .hasMessageContaining(StoreErrorCode.ALREADY_ADD.getMessage());
        verify(storeRepository, times(1)).existsByNameAndIsDeletedFalse("dup-store");
        verifyNoMoreInteractions(storeRepository);
        verifyNoInteractions(regionRepository, regionService, storeCategoryRepository);
    }

    @Test
    @DisplayName("createStore: Region 미조회 -> RegionException NOT_FOUND")
    void createStore_regionNotFound() {
        var dto = sampleCreateDTO("store-b", "서울 강남구 역삼동", "KOREAN");

        given(storeRepository.existsByNameAndIsDeletedFalse("store-b")).willReturn(false);
        given(storeCategoryRepository.findByCategory(anyString())).willReturn(Optional.of(StoreCategory.builder().category("KOREAN").build()));

        UUID regionId = UUID.randomUUID();
        given(regionService.parseAndSaveRegion(anyString())).willReturn(regionId);
        given(regionRepository.findById(regionId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> storeCommandService.createStore(dto, ownerPassport))
                .isInstanceOf(RegionException.class)
                .hasMessageContaining(RegionErrorCode.NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("updateStore: 성공적으로 수정 및 응답 반환")
    void updateStore_success() {
        UUID storeId = UUID.randomUUID();
        StoreCategory oldCategory = StoreCategory.builder().category("KOREAN").build();
        Store store = Store.builder()
                .name("old")
                .address("서울 강남구 삼성동")
                .phone("010-1111-2222")
                .content("c")
                .minPrice(1000)
                .deliveryTip(100)
                .operationHours("09-18")
                .closedDays("sun")
                .storeCategory(oldCategory)
                .region(Region.builder().siDo("서울").siGunGu("강남구").eupMyeonDong("삼성동").build())
                .ownerId(ownerId)
                .build();

        given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

        StoreCommonRequestDTO common = StoreCommonRequestDTO.builder()
                .name("new-name")
                .address("서울 강남구 역삼동")
                .category("CHINESE")
                .build();
        var dto = StoreRequestDTO.StoreUpdateRequestDTO.builder().storeCommonRequestDTO(common).build();

        given(storeRepository.existsByNameAndIsDeletedFalse("new-name")).willReturn(false);

        StoreCategory newCategory = StoreCategory.builder().category("CHINESE").build();
        given(storeCategoryRepository.findByCategory("CHINESE")).willReturn(Optional.empty());
        given(storeCategoryRepository.save(any(StoreCategory.class))).willReturn(newCategory);

        given(storeRepository.save(any(Store.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        StoreResponseDTO.StoreUpdateResponseDTO res = storeCommandService.updateStore(storeId, dto, ownerPassport);

        // then
        assertThat(res.getStoreCommonsBaseResponseDTO().getName()).isEqualTo("new-name");
        assertThat(res.getCategory()).isEqualTo("CHINESE");
        assertThat(store.getStoreCategory()).isEqualTo(newCategory);
        verify(storeRepository).save(any(Store.class));
    }

    @Test
    @DisplayName("updateStore: 권한 없음 -> UNAUTHORIZED_ACCESS")
    void updateStore_unauthorized() {
        UUID storeId = UUID.randomUUID();
        Store store = Store.builder()
                .name("old")
                .address("서울 강남구 삼성동")
                .phone("010")
                .content("c")
                .minPrice(1000)
                .deliveryTip(100)
                .operationHours("09-18")
                .closedDays("sun")
                .storeCategory(StoreCategory.builder().category("KOREAN").build())
                .region(Region.builder().siDo("서울").siGunGu("강남구").eupMyeonDong("삼성동").build())
                .ownerId(UUID.randomUUID()) // 다른 사용자
                .build();
        given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

        var dto = StoreRequestDTO.StoreUpdateRequestDTO.builder()
                .storeCommonRequestDTO(StoreCommonRequestDTO.builder().name("n").address("a").category("c").build())
                .build();

        assertThatThrownBy(() -> storeCommandService.updateStore(storeId, dto, ownerPassport))
                .isInstanceOf(StoreException.class)
                .hasMessageContaining(StoreErrorCode.UNAUTHORIZED_ACCESS.getMessage());
        verify(storeRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateStore: 중복 이름 -> ALREADY_ADD")
    void updateStore_duplicateName() {
        UUID storeId = UUID.randomUUID();
        Store store = Store.builder()
                .name("old")
                .address("a")
                .phone("p")
                .content("c")
                .minPrice(1)
                .deliveryTip(1)
                .operationHours("h")
                .closedDays("d")
                .storeCategory(StoreCategory.builder().category("KOREAN").build())
                .region(Region.builder().siDo("s").siGunGu("g").eupMyeonDong("e").build())
                .ownerId(ownerId)
                .build();
        given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

        var dto = StoreRequestDTO.StoreUpdateRequestDTO.builder()
                .storeCommonRequestDTO(StoreCommonRequestDTO.builder().name("dup").address("a").category("KOREAN").build())
                .build();
        given(storeRepository.existsByNameAndIsDeletedFalse("dup")).willReturn(true);

        assertThatThrownBy(() -> storeCommandService.updateStore(storeId, dto, ownerPassport))
                .isInstanceOf(StoreException.class)
                .hasMessageContaining(StoreErrorCode.ALREADY_ADD.getMessage());
        verify(storeRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteStore: 성공적으로 소프트 삭제")
    void deleteStore_success() {
        UUID storeId = UUID.randomUUID();
        Store store = Store.builder()
                .name("del")
                .address("addr")
                .phone("010")
                .content("c")
                .minPrice(1)
                .deliveryTip(1)
                .operationHours("h")
                .closedDays("d")
                .storeCategory(StoreCategory.builder().category("KOREAN").build())
                .region(Region.builder().siDo("s").siGunGu("g").eupMyeonDong("e").build())
                .ownerId(ownerId)
                .build();
        given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
        given(storeRepository.save(any(Store.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        storeCommandService.deleteStore(storeId, ownerPassport);

        // then
        assertThat(store.getIsDeleted()).isTrue();
        verify(storeRepository).save(store);
    }

    @Test
    @DisplayName("deleteStore: 권한 없음 -> UNAUTHORIZED_ACCESS")
    void deleteStore_unauthorized() {
        UUID storeId = UUID.randomUUID();
        Store store = Store.builder()
                .name("del")
                .address("addr")
                .phone("010")
                .content("c")
                .minPrice(1)
                .deliveryTip(1)
                .operationHours("h")
                .closedDays("d")
                .storeCategory(StoreCategory.builder().category("KOREAN").build())
                .region(Region.builder().siDo("s").siGunGu("g").eupMyeonDong("e").build())
                .ownerId(UUID.randomUUID())
                .build();
        given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

        assertThatThrownBy(() -> storeCommandService.deleteStore(storeId, ownerPassport))
                .isInstanceOf(StoreException.class)
                .hasMessageContaining(StoreErrorCode.UNAUTHORIZED_ACCESS.getMessage());
        verify(storeRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteStore: 존재하지 않음 -> NOT_FOUND")
    void deleteStore_notFound() {
        UUID storeId = UUID.randomUUID();
        given(storeRepository.findById(storeId)).willReturn(Optional.empty());
        assertThatThrownBy(() -> storeCommandService.deleteStore(storeId, ownerPassport))
                .isInstanceOf(StoreException.class)
                .hasMessageContaining(StoreErrorCode.NOT_FOUND.getMessage());
    }
}

