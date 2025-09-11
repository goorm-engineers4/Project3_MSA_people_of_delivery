package com.example.cloudfour.storeservice.domain.menu.service.command;

import com.example.cloudfour.storeservice.domain.menu.dto.PaymentEvent;
import com.example.cloudfour.storeservice.domain.menu.entity.Stock;
import com.example.cloudfour.storeservice.domain.menu.exception.StockErrorCode;
import com.example.cloudfour.storeservice.domain.menu.exception.StockException;
import com.example.cloudfour.storeservice.domain.menu.repository.StockRepository;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
@Retryable(
        value = OptimisticLockException.class,
        maxAttempts = 5,
        backoff = @Backoff(delay = 100)
)
public class StockCommandService {
    private final StockRepository stockRepository;
    private final org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;

    private static final String INVN_MENU_PREFIX = "invn:menu:";

    public void decreaseStock(UUID stockId, Long quantity){
        Stock stock = stockRepository.findByIdWithOptimisticLock(stockId).orElseThrow(()-> new StockException(StockErrorCode.NOT_FOUND));
        stock.decrease(quantity);
        try {
            redisTemplate.opsForValue().set(INVN_MENU_PREFIX + stock.getMenu().getId(), String.valueOf(stock.getQuantity()));
        } catch (Exception e) {
            log.error("Redis invn 캐시 갱신 실패 - menuId: {}", stock.getMenu().getId(), e);
        }
        log.info("재고 감소 - stockId: {}, quantity: {}, remaining: {}", stockId, quantity, stock.getQuantity());
    }

    public void increaseStock(UUID stockId, Long quantity){
        Stock stock = stockRepository.findByIdWithOptimisticLock(stockId).orElseThrow(()-> new StockException(StockErrorCode.NOT_FOUND));
        stock.increase(quantity);
        try {
            redisTemplate.opsForValue().set(INVN_MENU_PREFIX + stock.getMenu().getId(), String.valueOf(stock.getQuantity()));
        } catch (Exception e) {
            log.error("Redis invn 캐시 갱신 실패 - menuId: {}", stock.getMenu().getId(), e);
        }
        log.info("재고 증가 - stockId: {}, quantity: {}, total: {}", stockId, quantity, stock.getQuantity());
    }

    @Transactional
    public boolean decreaseListStock(List<PaymentEvent.PaymentCompletedEvent.OrderItem> orderItems){
        for (PaymentEvent.PaymentCompletedEvent.OrderItem item : orderItems) {
            Stock stock = stockRepository.findByIdWithOptimisticLock(item.getStockId())
                    .orElseThrow(() -> new StockException(StockErrorCode.NOT_FOUND));
            if (stock.getQuantity() < item.getQuantity()) {
                log.warn("재고 부족 - MenuId: {}, Required: {}, Available: {}", item.getMenuId(), item.getQuantity(), stock.getQuantity());
                throw new StockException(StockErrorCode.MINUS_FAILED);
            }
        }

        for (PaymentEvent.PaymentCompletedEvent.OrderItem item : orderItems) {
            Stock stock = stockRepository.findByIdWithOptimisticLock(item.getStockId())
                    .orElseThrow(() -> new StockException(StockErrorCode.NOT_FOUND));
            stock.decrease(item.getQuantity());
            try {
                redisTemplate.opsForValue().set(INVN_MENU_PREFIX + item.getMenuId(), String.valueOf(stock.getQuantity()));
            } catch (Exception e) {
                log.error("Redis invn 캐시 갱신 실패 - menuId: {}", item.getMenuId(), e);
            }
        }
        return true;
    }

    @Recover
    public void recover(OptimisticLockException e, UUID stockId, Long quantity) {
        log.error("재고 차감 재시도 중 충돌 무한 반복 - stockId={}, quantity={}", stockId, quantity, e);
        throw e;
    }
}
