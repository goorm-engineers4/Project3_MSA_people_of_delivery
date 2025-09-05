package com.example.cloudfour.storeservice.domain.menu.service;

import com.example.cloudfour.storeservice.util.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockRedisService {

    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    private static final String STOCK_PREFIX = "stock:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);

    public void cacheStock(UUID stockId, Long quantity) {
        String key = generateStockKey(stockId);
        try {
            redisUtil.setWithTtl(key, quantity.toString(), DEFAULT_TTL);
            log.debug("Redis에 stock 저장 - stockId: {}, quantity: {}", stockId, quantity);
        } catch (Exception e) {
            log.error("Redis에 stock 저장 실패 - stockId: {}", stockId, e);
        }
    }

    public Long getStockFromCache(UUID stockId) {
        String key = generateStockKey(stockId);
        try {
            String cachedValue = redisUtil.get(key);
            if (cachedValue != null) {
                return Long.valueOf(cachedValue);
            }
        } catch (Exception e) {
            log.error("Redis에 stock 조회 실패 - stockId: {}", stockId, e);
        }
        return null;
    }

    public void updateStockInCache(UUID stockId, Long newQuantity) {
        String key = generateStockKey(stockId);
        try {
            redisUtil.setWithTtl(key, newQuantity.toString(), DEFAULT_TTL);
            log.debug("Redis에서 stock 업데이트 성공 - stockId: {}, newQuantity: {}", stockId, newQuantity);
        } catch (Exception e) {
            log.error("Redis에서 stock 업데이트 실패 - stockId: {}", stockId, e);
        }
    }

    private String generateStockKey(UUID stockId) {
        return STOCK_PREFIX + stockId.toString();
    }
}
