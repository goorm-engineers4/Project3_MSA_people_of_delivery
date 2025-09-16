package com.example.cloudfour.storeservice.domain.menu.service;

import com.example.cloudfour.storeservice.domain.menu.dto.StockResponseDTO;
import com.example.cloudfour.storeservice.domain.menu.entity.Menu;
import com.example.cloudfour.storeservice.domain.menu.entity.Stock;
import com.example.cloudfour.storeservice.domain.menu.exception.MenuErrorCode;
import com.example.cloudfour.storeservice.domain.menu.exception.MenuException;
import com.example.cloudfour.storeservice.domain.menu.exception.StockErrorCode;
import com.example.cloudfour.storeservice.domain.menu.exception.StockException;
import com.example.cloudfour.storeservice.domain.menu.repository.MenuRepository;
import com.example.cloudfour.storeservice.domain.menu.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisInventoryServiceTest {

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private MenuRepository menuRepository;
    @Mock private StockRepository stockRepository;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks private RedisInventoryService redisInventoryService;

    private UUID menuId;
    private UUID stockId;
    private UUID orderId;
    private Menu menu;
    private Stock stock;

    @BeforeEach
    void setUp() {
        menuId = UUID.randomUUID();
        stockId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        menu = mock(Menu.class);
        stock = mock(Stock.class);
        lenient().when(menu.getId()).thenReturn(menuId);
        lenient().when(menu.getStock()).thenReturn(stock);
        lenient().when(stock.getId()).thenReturn(stockId);
        lenient().when(stock.getQuantity()).thenReturn(100L);

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("reserveStock: 캐시 미존재 시 로드 후 성공")
    void reserveStock_loadsCache_thenSuccess() {
        given(redisTemplate.hasKey("invn:menu:" + menuId)).willReturn(false);
        given(menuRepository.findById(menuId)).willReturn(Optional.of(menu));
        given(stockRepository.findByIdWithOptimisticLock(stockId)).willReturn(Optional.of(stock));

        @SuppressWarnings("unchecked")
        List<Object> scriptResult = Arrays.asList(0, "SUCCESS", 97);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(scriptResult);

        StockResponseDTO.StockReserveResponseDTO res = redisInventoryService.reserveStock(menuId, 3L, orderId);

        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getRemainingStock()).isEqualTo(97L);
        verify(valueOps).set("invn:menu:" + menuId, String.valueOf(100L));
    }

    @Test
    @DisplayName("reserveStock: 스크립트 - 메뉴 없음")
    void reserveStock_menuNotFoundFromScript() {
        given(redisTemplate.hasKey("invn:menu:" + menuId)).willReturn(true);
        @SuppressWarnings("unchecked")
        List<Object> scriptResult = Arrays.asList(-1, "MENU_NOT_FOUND");
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(scriptResult);

        StockResponseDTO.StockReserveResponseDTO res = redisInventoryService.reserveStock(menuId, 1L, orderId);
        assertThat(res.isSuccess()).isFalse();
        assertThat(res.getMessage()).isEqualTo("Menu not found");
    }

    @Test
    @DisplayName("reserveStock: 스크립트 - 재고 부족")
    void reserveStock_insufficient() {
        given(redisTemplate.hasKey("invn:menu:" + menuId)).willReturn(true);
        @SuppressWarnings("unchecked")
        List<Object> scriptResult = Arrays.asList(-2, "INSUFFICIENT_STOCK", 4);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(scriptResult);

        StockResponseDTO.StockReserveResponseDTO res = redisInventoryService.reserveStock(menuId, 10L, orderId);
        assertThat(res.isSuccess()).isFalse();
        assertThat(res.getRemainingStock()).isEqualTo(4L);
        assertThat(res.getMessage()).isEqualTo("Insufficient stock");
    }

    @Test
    @DisplayName("reserveStock: 스크립트 - 기타 에러 코드")
    void reserveStock_defaultError() {
        given(redisTemplate.hasKey("invn:menu:" + menuId)).willReturn(true);
        @SuppressWarnings("unchecked")
        List<Object> scriptResult = Arrays.asList(123, "ERR");
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(scriptResult);

        StockResponseDTO.StockReserveResponseDTO res = redisInventoryService.reserveStock(menuId, 1L, orderId);
        assertThat(res.isSuccess()).isFalse();
        assertThat(res.getMessage()).isEqualTo("ERR");
    }

    @Test
    @DisplayName("reserveStock: Redis 실행 중 예외 → error 응답")
    void reserveStock_executeThrows() {
        given(redisTemplate.hasKey("invn:menu:" + menuId)).willReturn(true);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new RuntimeException("boom"));

        StockResponseDTO.StockReserveResponseDTO res = redisInventoryService.reserveStock(menuId, 1L, orderId);
        assertThat(res.isSuccess()).isFalse();
        assertThat(res.getMessage()).isEqualTo("Redis operation failed");
    }

    @Test
    @DisplayName("reserveStock: ensureStockInRedis - 메뉴 없음 예외 전파")
    void reserveStock_ensure_menuNotFound() {
        given(redisTemplate.hasKey("invn:menu:" + menuId)).willReturn(false);
        given(menuRepository.findById(menuId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> redisInventoryService.reserveStock(menuId, 1L, orderId))
                .isInstanceOf(MenuException.class)
                .hasMessageContaining(MenuErrorCode.NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("reserveStock: ensureStockInRedis - 재고 없음 예외 전파")
    void reserveStock_ensure_stockNotFound() {
        given(redisTemplate.hasKey("invn:menu:" + menuId)).willReturn(false);
        given(menuRepository.findById(menuId)).willReturn(Optional.of(menu));
        given(stockRepository.findByIdWithOptimisticLock(stockId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> redisInventoryService.reserveStock(menuId, 1L, orderId))
                .isInstanceOf(StockException.class)
                .hasMessageContaining(StockErrorCode.NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("reserveStock: ensureStockInRedis - Redis set 실패 → INTERNAL_ERROR")
    void reserveStock_ensure_redisSetFails() {
        given(redisTemplate.hasKey("invn:menu:" + menuId)).willReturn(false);
        given(menuRepository.findById(menuId)).willReturn(Optional.of(menu));
        given(stockRepository.findByIdWithOptimisticLock(stockId)).willReturn(Optional.of(stock));
        doThrow(new RuntimeException("redis down")).when(valueOps).set(anyString(), anyString());

        assertThatThrownBy(() -> redisInventoryService.reserveStock(menuId, 1L, orderId))
                .isInstanceOf(StockException.class)
                .hasMessageContaining(StockErrorCode.INTERNAL_ERROR.getMessage());
    }

    @Test
    @DisplayName("releaseReservation: 성공 및 스크립트 호출")
    void releaseReservation_success() {
        @SuppressWarnings("unchecked")
        List<Object> scriptResult = Arrays.asList(0, "SUCCESS");
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(scriptResult);

        redisInventoryService.releaseReservation(orderId);

        ArgumentCaptor<List> keysCaptor = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate).execute(any(RedisScript.class), keysCaptor.capture(), any(Object[].class));
        List keys = keysCaptor.getValue();
        assertThat(keys).hasSize(1);
        assertThat(keys.get(0)).isEqualTo("order:rsrv:" + orderId);
    }

    @Test
    @DisplayName("releaseReservation: Redis 예외 발생해도 무시")
    void releaseReservation_redisThrows() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new RuntimeException("boom"));
        // should not throw
        redisInventoryService.releaseReservation(orderId);
    }

    @Test
    @DisplayName("getAvailability: 성공 - 값 매핑")
    void getAvailability_success() {
        @SuppressWarnings("unchecked")
        List<Object> scriptResult = Arrays.asList(0, 40, 50, 10);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(scriptResult);

        StockResponseDTO.StockAvailabilityResponseDTO res = redisInventoryService.getAvailability(menuId);
        assertThat(res.getMenuId()).isEqualTo(menuId);
        assertThat(res.getBaseQuantity()).isEqualTo(50L);
        assertThat(res.getAvailableQuantity()).isEqualTo(40L);
        assertThat(res.getReservedQuantity()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getAvailability: 메뉴 없음 → null 필드")
    void getAvailability_menuNotFound() {
        @SuppressWarnings("unchecked")
        List<Object> scriptResult = Arrays.asList(-1, "MENU_NOT_FOUND");
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(scriptResult);

        StockResponseDTO.StockAvailabilityResponseDTO res = redisInventoryService.getAvailability(menuId);
        assertThat(res.getMenuId()).isEqualTo(menuId);
        assertThat(res.getBaseQuantity()).isNull();
        assertThat(res.getAvailableQuantity()).isNull();
        assertThat(res.getReservedQuantity()).isNull();
    }

    @Test
    @DisplayName("getAvailability: Redis 예외 → null 필드")
    void getAvailability_redisThrows() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new RuntimeException("boom"));

        StockResponseDTO.StockAvailabilityResponseDTO res = redisInventoryService.getAvailability(menuId);
        assertThat(res.getMenuId()).isEqualTo(menuId);
        assertThat(res.getBaseQuantity()).isNull();
        assertThat(res.getAvailableQuantity()).isNull();
        assertThat(res.getReservedQuantity()).isNull();
    }
}

