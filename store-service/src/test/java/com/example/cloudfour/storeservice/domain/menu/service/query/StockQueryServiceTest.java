package com.example.cloudfour.storeservice.domain.menu.service.query;

import com.example.cloudfour.storeservice.domain.menu.dto.StockResponseDTO;
import com.example.cloudfour.storeservice.domain.menu.entity.Menu;
import com.example.cloudfour.storeservice.domain.menu.entity.Stock;
import com.example.cloudfour.storeservice.domain.menu.exception.MenuErrorCode;
import com.example.cloudfour.storeservice.domain.menu.exception.MenuException;
import com.example.cloudfour.storeservice.domain.menu.repository.MenuRepository;
import com.example.cloudfour.storeservice.domain.menu.repository.StockRepository;
import com.example.cloudfour.storeservice.domain.menu.service.RedisInventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockQueryServiceTest {

    @Mock private StockRepository stockRepository;
    @Mock private MenuRepository menuRepository;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private RedisInventoryService redisInventoryService;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks private StockQueryService stockQueryService;

    private UUID menuId;
    private UUID stockId;
    private Menu menu;
    private Stock stockRefOnMenu; // menu.getStock()

    @BeforeEach
    void setUp() {
        menuId = UUID.randomUUID();
        stockId = UUID.randomUUID();

        menu = mock(Menu.class);
        stockRefOnMenu = mock(Stock.class);
        lenient().when(stockRefOnMenu.getId()).thenReturn(stockId);
        lenient().when(menu.getStock()).thenReturn(stockRefOnMenu);
        lenient().when(menu.getId()).thenReturn(menuId);

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("getMenuStockTest: 성공 - DB 조회 반환")
    void getMenuStockTest_success() {
        given(menuRepository.findById(menuId)).willReturn(Optional.of(menu));

        Stock stockFromDb = mock(Stock.class);
        lenient().when(stockFromDb.getId()).thenReturn(stockId);
        lenient().when(stockFromDb.getMenu()).thenReturn(menu);
        lenient().when(stockFromDb.getQuantity()).thenReturn(15L);
        lenient().when(stockRepository.findByIdWithOptimisticLock(stockId)).thenReturn(Optional.of(stockFromDb));

        StockResponseDTO.StockCacheResponseDTO res = stockQueryService.getMenuStockTest(menuId);

        assertThat(res.getStockId()).isEqualTo(stockId);
        assertThat(res.getMenuId()).isEqualTo(menuId);
        assertThat(res.getQuantity()).isEqualTo(15L);
    }

    @Test
    @DisplayName("getMenuStockTest: 메뉴 없음 -> MenuException NOT_FOUND")
    void getMenuStockTest_menuNotFound() {
        given(menuRepository.findById(menuId)).willReturn(Optional.empty());
        assertThatThrownBy(() -> stockQueryService.getMenuStockTest(menuId))
                .isInstanceOf(MenuException.class)
                .hasMessageContaining(MenuErrorCode.NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("getMenuStock: 캐시 히트 - Redis 값 반환")
    void getMenuStock_cacheHit() {
        given(menuRepository.findById(menuId)).willReturn(Optional.of(menu));
        given(valueOps.get("invn:menu:" + menuId)).willReturn("12");

        StockResponseDTO.StockCacheResponseDTO res = stockQueryService.getMenuStock(menuId);

        assertThat(res.getMenuId()).isEqualTo(menuId);
        assertThat(res.getStockId()).isEqualTo(stockId);
        assertThat(res.getQuantity()).isEqualTo(12L);
        verify(stockRepository, never()).findByIdWithOptimisticLock(any());
    }

    @Test
    @DisplayName("getMenuStock: 캐시 미스 - DB 조회 후 캐시 저장")
    void getMenuStock_cacheMiss_dbFetch() {
        given(menuRepository.findById(menuId)).willReturn(Optional.of(menu));
        given(valueOps.get("invn:menu:" + menuId)).willReturn(null);

        Stock stockFromDb = mock(Stock.class);
        when(stockFromDb.getId()).thenReturn(stockId);
        when(stockFromDb.getMenu()).thenReturn(menu);
        when(stockFromDb.getQuantity()).thenReturn(30L);
        given(stockRepository.findByIdWithOptimisticLock(stockId)).willReturn(Optional.of(stockFromDb));

        StockResponseDTO.StockCacheResponseDTO res = stockQueryService.getMenuStock(menuId);

        assertThat(res.getStockId()).isEqualTo(stockId);
        assertThat(res.getMenuId()).isEqualTo(menuId);
        assertThat(res.getQuantity()).isEqualTo(30L);
        verify(valueOps).set("invn:menu:" + menuId, String.valueOf(30L));
    }

    @Test
    @DisplayName("getMenuStock: 메뉴 없음 -> MenuException NOT_FOUND")
    void getMenuStock_menuNotFound() {
        given(menuRepository.findById(menuId)).willReturn(Optional.empty());
        assertThatThrownBy(() -> stockQueryService.getMenuStock(menuId))
                .isInstanceOf(MenuException.class)
                .hasMessageContaining(MenuErrorCode.NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("getMenuStockAvailability: 캐시 히트 + 가용/예약 반환")
    void getMenuStockAvailability_cacheHit_withAvail() {
        given(menuRepository.findById(menuId)).willReturn(Optional.of(menu));
        given(valueOps.get("invn:menu:" + menuId)).willReturn("50");

        StockResponseDTO.StockAvailabilityResponseDTO avail = StockResponseDTO.StockAvailabilityResponseDTO.builder()
                .menuId(menuId)
                .baseQuantity(50L)
                .availableQuantity(40L)
                .reservedQuantity(10L)
                .build();
        given(redisInventoryService.getAvailability(menuId)).willReturn(avail);

        StockResponseDTO.StockAvailabilityResponseDTO res = stockQueryService.getMenuStockAvailability(menuId);

        assertThat(res.getStockId()).isEqualTo(stockId);
        assertThat(res.getMenuId()).isEqualTo(menuId);
        assertThat(res.getBaseQuantity()).isEqualTo(50L);
        assertThat(res.getAvailableQuantity()).isEqualTo(40L);
        assertThat(res.getReservedQuantity()).isEqualTo(10L);
        verify(stockRepository, never()).findByIdWithOptimisticLock(any());
    }

    @Test
    @DisplayName("getMenuStockAvailability: 캐시 미스 → DB 값과 avail nulls 처리")
    void getMenuStockAvailability_cacheMiss_fallbackDb() {
        given(menuRepository.findById(menuId)).willReturn(Optional.of(menu));
        given(valueOps.get("invn:menu:" + menuId)).willReturn(null);

        Stock stockFromDb = mock(Stock.class);
        lenient().when(stockFromDb.getId()).thenReturn(stockId);
        lenient().when(stockFromDb.getMenu()).thenReturn(menu);
        lenient().when(stockFromDb.getQuantity()).thenReturn(77L);
        given(stockRepository.findByIdWithOptimisticLock(stockId)).willReturn(Optional.of(stockFromDb));

        StockResponseDTO.StockAvailabilityResponseDTO avail = StockResponseDTO.StockAvailabilityResponseDTO.builder()
                .menuId(menuId)
                .baseQuantity(null)
                .availableQuantity(null)
                .reservedQuantity(null)
                .build();
        given(redisInventoryService.getAvailability(menuId)).willReturn(avail);

        StockResponseDTO.StockAvailabilityResponseDTO res = stockQueryService.getMenuStockAvailability(menuId);

        assertThat(res.getStockId()).isEqualTo(stockId);
        assertThat(res.getMenuId()).isEqualTo(menuId);
        assertThat(res.getBaseQuantity()).isEqualTo(77L);
        assertThat(res.getAvailableQuantity()).isEqualTo(77L);
        assertThat(res.getReservedQuantity()).isEqualTo(0L);
        verify(valueOps).set("invn:menu:" + menuId, String.valueOf(77L));
    }

    @Test
    @DisplayName("getMenuStockAvailability: 메뉴 없음 -> MenuException NOT_FOUND")
    void getMenuStockAvailability_menuNotFound() {
        given(menuRepository.findById(menuId)).willReturn(Optional.empty());
        assertThatThrownBy(() -> stockQueryService.getMenuStockAvailability(menuId))
                .isInstanceOf(MenuException.class)
                .hasMessageContaining(MenuErrorCode.NOT_FOUND.getMessage());
    }
}

