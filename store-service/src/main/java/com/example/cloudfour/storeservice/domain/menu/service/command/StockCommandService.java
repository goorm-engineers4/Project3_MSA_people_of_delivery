package com.example.cloudfour.storeservice.domain.menu.service.command;

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

    @Recover
    public void recover(OptimisticLockException e, UUID stockId, Long quantity) {
        log.error("재고 차감 재시도 중 충돌 무한 반복 - stockId={}, quantity={}", stockId, quantity, e);
        throw e;
    }
}
