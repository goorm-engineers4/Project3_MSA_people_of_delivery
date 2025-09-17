package com.example.cloudfour.storeservice.domain.store.service.query;

import com.example.cloudfour.modulecommon.dto.Passport;
import com.example.cloudfour.storeservice.client.UserClient;
import com.example.cloudfour.storeservice.domain.collection.document.StoreDocument;
import com.example.cloudfour.storeservice.domain.collection.repository.query.StoreSearchRepository;
import com.example.cloudfour.storeservice.domain.common.RegionResponseDTO;
import com.example.cloudfour.storeservice.domain.common.StoreCartResponseDTO;
import com.example.cloudfour.storeservice.domain.region.exception.RegionErrorCode;
import com.example.cloudfour.storeservice.domain.region.exception.RegionException;
import com.example.cloudfour.storeservice.domain.store.dto.StoreResponseDTO;
import com.example.cloudfour.storeservice.domain.store.entity.Store;
import com.example.cloudfour.storeservice.domain.store.entity.StoreCategory;
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
class StoreQueryServiceTest {

    @Mock private StoreSearchRepository storeMongoRepository;
    @Mock private StoreRepository query;
    @Mock private UserClient userClient;

    @InjectMocks private StoreQueryService storeQueryService;

    private Passport passport;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        passport = Passport.builder().userId(userId).build();
    }

    private RegionResponseDTO sampleRegion() {
        return RegionResponseDTO.builder()
                .siDo("서울")
                .siGunGu("강남구")
                .eupMyeonDong("역삼동")
                .build();
    }

    private StoreDocument sampleDoc(String name, LocalDateTime createdAt) {
        return StoreDocument.builder()
                .storeId(UUID.randomUUID())
                .userId(userId)
                .name(name)
                .address("서울 강남구 역삼동")
                .phone("010-1234-5678")
                .content("content")
                .minPrice(1000)
                .deliveryTip(100)
                .rating(4.5f)
                .likeCount(10)
                .reviewCount(5)
                .siDo("서울")
                .siGunGu("강남구")
                .eupMyeonDong("역삼동")
                .pictureURL("pic")
                .createdAt(createdAt)
                .storeCategory(StoreDocument.StoreCategory.builder()
                        .id(UUID.randomUUID())
                        .storeCategoryName("KOREAN")
                        .build())
                .build();
    }

    @Test
    @DisplayName("getAllStores: 성공 - 키워드/지역 기준 페이지 조회")
    void getAllStores_success() {
        // given
        given(userClient.getUserRegion(userId)).willReturn(sampleRegion());
        LocalDateTime now = LocalDateTime.now();
        StoreDocument d1 = sampleDoc("A", now.minusMinutes(5));
        StoreDocument d2 = sampleDoc("B", now.minusMinutes(1));
        Slice<StoreDocument> slice = new SliceImpl<>(List.of(d1, d2), PageRequest.of(0, 2), true);
        given(storeMongoRepository.findAllStoreByKeyWordAndRegion(eq("chicken"), any(), any(), eq("서울"), eq("강남구"), eq("역삼동")))
                .willReturn(slice);

        // when
        StoreResponseDTO.StoreCursorListResponseDTO res = storeQueryService.getAllStores(null, 2, "chicken", passport);

        // then
        assertThat(res.getStoreList()).hasSize(2);
        assertThat(res.getStoreList().get(0).getStoreCommonsBaseResponseDTO().getName()).isEqualTo("A");
        assertThat(res.getStoreList().get(1).getStoreCommonsBaseResponseDTO().getName()).isEqualTo("B");
        assertThat(res.getNextCursor()).isEqualTo(d2.getCreatedAt());
    }

    @Test
    @DisplayName("getAllStores: 권한 없음 -> UNAUTHORIZED_ACCESS")
    void getAllStores_unauthorized() {
        assertThatThrownBy(() -> storeQueryService.getAllStores(null, 10, null, null))
                .isInstanceOf(StoreException.class)
                .hasMessageContaining(StoreErrorCode.UNAUTHORIZED_ACCESS.getMessage());
        verifyNoInteractions(userClient, storeMongoRepository);
    }

    @Test
    @DisplayName("getAllStores: 사용자 지역 없음 -> RegionException NOT_FOUND")
    void getAllStores_regionNotFound() {
        given(userClient.getUserRegion(userId)).willReturn(null);
        assertThatThrownBy(() -> storeQueryService.getAllStores(null, 10, null, passport))
                .isInstanceOf(RegionException.class)
                .hasMessageContaining(RegionErrorCode.NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("getStoresByCategory: 성공 - 카테고리/지역 기준 페이지 조회")
    void getStoresByCategory_success() {
        UUID categoryId = UUID.randomUUID();
        given(userClient.getUserRegion(userId)).willReturn(sampleRegion());
        LocalDateTime now = LocalDateTime.now();
        StoreDocument d1 = sampleDoc("C", now.minusMinutes(3));
        Slice<StoreDocument> slice = new SliceImpl<>(List.of(d1), PageRequest.of(0, 1), false);
        given(storeMongoRepository.findAllStoreByCategoryAndCursor(eq(categoryId), any(), any(), eq("서울"), eq("강남구"), eq("역삼동")))
                .willReturn(slice);

        StoreResponseDTO.StoreCursorListResponseDTO res = storeQueryService.getStoresByCategory(categoryId, null, 1, passport);
        assertThat(res.getStoreList()).hasSize(1);
        assertThat(res.getStoreList().get(0).getStoreCommonsBaseResponseDTO().getName()).isEqualTo("C");
        assertThat(res.getNextCursor()).isNull();
    }

    @Test
    @DisplayName("getStoresByCategory: 권한 없음 -> UNAUTHORIZED_ACCESS")
    void getStoresByCategory_unauthorized() {
        assertThatThrownBy(() -> storeQueryService.getStoresByCategory(UUID.randomUUID(), null, 10, null))
                .isInstanceOf(StoreException.class)
                .hasMessageContaining(StoreErrorCode.UNAUTHORIZED_ACCESS.getMessage());
        verifyNoInteractions(userClient, storeMongoRepository);
    }

    @Test
    @DisplayName("getStoresByCategory: 사용자 지역 없음 -> RegionException NOT_FOUND")
    void getStoresByCategory_regionNotFound() {
        given(userClient.getUserRegion(userId)).willReturn(null);
        assertThatThrownBy(() -> storeQueryService.getStoresByCategory(UUID.randomUUID(), null, 10, passport))
                .isInstanceOf(RegionException.class)
                .hasMessageContaining(RegionErrorCode.NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("getStoreById: 성공 - 상세 조회")
    void getStoreById_success() {
        UUID storeId = UUID.randomUUID();
        StoreDocument doc = sampleDoc("Detail", LocalDateTime.now());
        given(storeMongoRepository.findStoreByStoreId(storeId)).willReturn(Optional.of(doc));

        StoreResponseDTO.StoreDetailResponseDTO res = storeQueryService.getStoreById(storeId, passport);
        assertThat(res.getStoreCommonsBaseResponseDTO().getName()).isEqualTo("Detail");
        assertThat(res.getUserId()).isEqualTo(userId);
        assertThat(res.getStoreCommonMainResponseDTO().getCategory()).isEqualTo("KOREAN");
    }

    @Test
    @DisplayName("getStoreById: 권한 없음 -> UNAUTHORIZED_ACCESS")
    void getStoreById_unauthorized() {
        assertThatThrownBy(() -> storeQueryService.getStoreById(UUID.randomUUID(), null))
                .isInstanceOf(StoreException.class)
                .hasMessageContaining(StoreErrorCode.UNAUTHORIZED_ACCESS.getMessage());
    }

    @Test
    @DisplayName("getStoreById: 가게 없음 -> NOT_FOUND")
    void getStoreById_notFound() {
        UUID storeId = UUID.randomUUID();
        given(storeMongoRepository.findStoreByStoreId(storeId)).willReturn(Optional.empty());
        assertThatThrownBy(() -> storeQueryService.getStoreById(storeId, passport))
                .isInstanceOf(StoreException.class)
                .hasMessageContaining(StoreErrorCode.NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("findStore: 성공 - 장바구니용 응답 반환")
    void findStore_success() {
        UUID storeId = UUID.randomUUID();
        Store store = Store.builder()
                .name("S")
                .address("A")
                .phone("P")
                .content("C")
                .minPrice(1)
                .deliveryTip(1)
                .operationHours("H")
                .closedDays("D")
                .storeCategory(StoreCategory.builder().category("KOREAN").build())
                .region(com.example.cloudfour.storeservice.domain.region.entity.Region.builder().siDo("s").siGunGu("g").eupMyeonDong("e").build())
                .ownerId(userId)
                .build();
        given(query.findByIdAndIsDeletedFalse(storeId)).willReturn(Optional.of(store));

        StoreCartResponseDTO res = storeQueryService.findStore(storeId);
        assertThat(res.getUserId()).isEqualTo(userId);
        assertThat(res.getName()).isEqualTo("S");
        assertThat(res.getStoreId()).isEqualTo(store.getId());
    }

    @Test
    @DisplayName("findStore: 가게 없음 -> NOT_FOUND")
    void findStore_notFound() {
        UUID storeId = UUID.randomUUID();
        given(query.findByIdAndIsDeletedFalse(storeId)).willReturn(Optional.empty());
        assertThatThrownBy(() -> storeQueryService.findStore(storeId))
                .isInstanceOf(StoreException.class)
                .hasMessageContaining(StoreErrorCode.NOT_FOUND.getMessage());
    }
}

