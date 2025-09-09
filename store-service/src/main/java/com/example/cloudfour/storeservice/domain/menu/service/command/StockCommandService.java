package com.example.cloudfour.storeservice.domain.menu.service.command;

import com.example.cloudfour.storeservice.domain.menu.dto.PaymentEvent;
import com.example.cloudfour.storeservice.domain.menu.dto.StockResponseDTO;
import com.example.cloudfour.storeservice.domain.menu.entity.Stock;
import com.example.cloudfour.storeservice.domain.menu.exception.StockErrorCode;
import com.example.cloudfour.storeservice.domain.menu.exception.StockException;
import com.example.cloudfour.storeservice.domain.menu.repository.StockRepository;
import com.example.cloudfour.storeservice.domain.menu.service.StockRedisService;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
    private final StockRedisService stockRedisService;

    public void decreaseStock(UUID stockId, Long quantity){
        Stock stock = stockRepository.findByIdWithOptimisticLock(stockId).orElseThrow(()-> new StockException(StockErrorCode.NOT_FOUND));
        stock.decrease(quantity);
        stockRedisService.updateStockInCache(stockId, stock.getQuantity());
        log.info("재고 감소 - stockId: {}, quantity: {}, remaining: {}", stockId, quantity, stock.getQuantity());
    }

    public void increaseStock(UUID stockId, Long quantity){
        Stock stock = stockRepository.findByIdWithOptimisticLock(stockId).orElseThrow(()-> new StockException(StockErrorCode.NOT_FOUND));
        stock.increase(quantity);
        stockRedisService.updateStockInCache(stockId, stock.getQuantity());
        log.info("재고 증가 - stockId: {}, quantity: {}, total: {}", stockId, quantity, stock.getQuantity());

    }

    public boolean decreaseListStock(List<PaymentEvent.PaymentCompletedEvent.OrderItem> orderItems){
        List<StockResponseDTO.StockUpdateInfo> stockUpdates = new ArrayList<>();
        for (PaymentEvent.PaymentCompletedEvent.OrderItem item : orderItems) {
            Stock stock = stockRepository.findByIdWithOptimisticLock(item.getMenuId())
                    .orElseThrow(()-> new StockException(StockErrorCode.NOT_FOUND));

            if (stock.getQuantity() < item.getQuantity()) {
                    log.warn("재고 부족 - MenuId: {}, Required: {}, Available: {}",
                        item.getMenuId(), item.getQuantity(), stock.getQuantity());
                return false;
            }

            stock.decrease(item.getQuantity());
            stockRedisService.updateStockInCache(item.getStockId(), stock.getQuantity());
            stockUpdates.add(StockResponseDTO.StockUpdateInfo.builder()
                    .stockId(stock.getId())
                    .quantity(stock.getQuantity())
                    .build());
        }

        for (StockResponseDTO.StockUpdateInfo updateInfo : stockUpdates) {
            stockRedisService.updateStockInCache(updateInfo.getStockId(), updateInfo.getQuantity());
        }

        return true;
    }

    @Recover
    public void recover(OptimisticLockException e, UUID stockId, Long quantity) {
        log.error("재고 차감 재시도 중 충돌 무한 반복 - stockId={}, quantity={}", stockId, quantity, e);
        throw e;
    }
}
