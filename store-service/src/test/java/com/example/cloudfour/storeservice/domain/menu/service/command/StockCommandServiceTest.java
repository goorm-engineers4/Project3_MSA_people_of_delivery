package com.example.cloudfour.storeservice.domain.menu.service.command;

import com.example.cloudfour.storeservice.domain.menu.dto.PaymentEvent;
import com.example.cloudfour.storeservice.domain.menu.entity.Menu;
import com.example.cloudfour.storeservice.domain.menu.entity.Stock;
import com.example.cloudfour.storeservice.domain.menu.exception.StockErrorCode;
import com.example.cloudfour.storeservice.domain.menu.exception.StockException;
import com.example.cloudfour.storeservice.domain.menu.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockCommandServiceTest {

    @Mock private StockRepository stockRepository;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks private StockCommandService stockCommandService;

    private UUID stockId;
    private UUID menuId;
    private Stock stock;

    @BeforeEach
    void setUp() {
        stockId = UUID.randomUUID();
        menuId = UUID.randomUUID();

        // Stock with a mock Menu to avoid NPE when accessing stock.getMenu().getId()
        stock = Stock.builder().quantity(10L).build();
        Menu menu = mock(Menu.class);
        lenient().when(menu.getId()).thenReturn(menuId);
        stock.setMenu(menu);

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("decreaseStock: 성공 - 수량 감소 및 Redis 갱신")
    void decreaseStock_success() {
        given(stockRepository.findByIdWithOptimisticLock(stockId)).willReturn(Optional.of(stock));

        stockCommandService.decreaseStock(stockId, 3L);

        assertThat(stock.getQuantity()).isEqualTo(7L);
        verify(valueOps).set("invn:menu:" + menuId, String.valueOf(7L));
    }

    @Test
    @DisplayName("decreaseStock: 재고 없음 -> NOT_FOUND")
    void decreaseStock_notFound() {
        given(stockRepository.findByIdWithOptimisticLock(stockId)).willReturn(Optional.empty());
        assertThatThrownBy(() -> stockCommandService.decreaseStock(stockId, 1L))
                .isInstanceOf(StockException.class)
                .hasMessageContaining(StockErrorCode.NOT_FOUND.getMessage());
        verify(valueOps, never()).set(anyString(), anyString());
    }

    @Test
    @DisplayName("decreaseStock: Redis 실패해도 예외 전파 안함")
    void decreaseStock_redisFailureIgnored() {
        given(stockRepository.findByIdWithOptimisticLock(stockId)).willReturn(Optional.of(stock));
        doThrow(new RuntimeException("redis down")).when(valueOps).set(anyString(), anyString());

        stockCommandService.decreaseStock(stockId, 2L);

        assertThat(stock.getQuantity()).isEqualTo(8L);
        // set은 호출되지만 예외는 서비스에서 잡음
        verify(valueOps).set("invn:menu:" + menuId, String.valueOf(8L));
    }

    @Test
    @DisplayName("increaseStock: 성공 - 수량 증가 및 Redis 갱신")
    void increaseStock_success() {
        given(stockRepository.findByIdWithOptimisticLock(stockId)).willReturn(Optional.of(stock));

        stockCommandService.increaseStock(stockId, 5L);

        assertThat(stock.getQuantity()).isEqualTo(15L);
        verify(valueOps).set("invn:menu:" + menuId, String.valueOf(15L));
    }

    @Test
    @DisplayName("decreaseListStock: 성공 - 모든 항목 검증 후 차감 및 Redis 갱신")
    void decreaseListStock_success() {
        UUID stockId1 = UUID.randomUUID();
        UUID stockId2 = UUID.randomUUID();
        UUID menuId1 = UUID.randomUUID();
        UUID menuId2 = UUID.randomUUID();

        Stock s1 = Stock.builder().quantity(10L).build();
        Menu m1 = mock(Menu.class); lenient().when(m1.getId()).thenReturn(menuId1); s1.setMenu(m1);
        Stock s2 = Stock.builder().quantity(20L).build();
        Menu m2 = mock(Menu.class); lenient().when(m2.getId()).thenReturn(menuId2); s2.setMenu(m2);

        // findByIdWithOptimisticLock is called twice per item (validation + actual decrease)
        given(stockRepository.findByIdWithOptimisticLock(any())).willAnswer(inv -> {
            UUID id = inv.getArgument(0);
            if (id.equals(stockId1)) return Optional.of(s1);
            if (id.equals(stockId2)) return Optional.of(s2);
            return Optional.empty();
        });

        PaymentEvent.PaymentCompletedEvent.OrderItem item1 = PaymentEvent.PaymentCompletedEvent.OrderItem.builder()
                .menuId(menuId1).stockId(stockId1).menuName("m1").quantity(3L).price(1000).build();
        PaymentEvent.PaymentCompletedEvent.OrderItem item2 = PaymentEvent.PaymentCompletedEvent.OrderItem.builder()
                .menuId(menuId2).stockId(stockId2).menuName("m2").quantity(5L).price(2000).build();

        boolean result = stockCommandService.decreaseListStock(List.of(item1, item2));

        assertThat(result).isTrue();
        assertThat(s1.getQuantity()).isEqualTo(7L);
        assertThat(s2.getQuantity()).isEqualTo(15L);
        verify(valueOps).set("invn:menu:" + menuId1, String.valueOf(7L));
        verify(valueOps).set("invn:menu:" + menuId2, String.valueOf(15L));
    }

    @Test
    @DisplayName("decreaseListStock: 검증에서 재고 부족 -> MINUS_FAILED")
    void decreaseListStock_insufficient() {
        UUID sId = UUID.randomUUID();
        UUID mId = UUID.randomUUID();
        Stock s = Stock.builder().quantity(2L).build();
        Menu m = mock(Menu.class); lenient().when(m.getId()).thenReturn(mId); s.setMenu(m);

        given(stockRepository.findByIdWithOptimisticLock(sId)).willReturn(Optional.of(s));

        PaymentEvent.PaymentCompletedEvent.OrderItem item = PaymentEvent.PaymentCompletedEvent.OrderItem.builder()
                .menuId(mId).stockId(sId).quantity(5L).price(1000).menuName("m").build();

        assertThatThrownBy(() -> stockCommandService.decreaseListStock(List.of(item)))
                .isInstanceOf(StockException.class)
                .hasMessageContaining(StockErrorCode.MINUS_FAILED.getMessage());

        // Redis 갱신 시도 없음
        verify(valueOps, never()).set(anyString(), anyString());
    }
}

