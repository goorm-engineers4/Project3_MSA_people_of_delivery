package com.example.cloudfour.storeservice.domain.menu.service.query;

import com.example.cloudfour.storeservice.domain.menu.converter.StockConverter;
import com.example.cloudfour.storeservice.domain.menu.dto.StockResponseDTO;
import com.example.cloudfour.storeservice.domain.menu.entity.Menu;
import com.example.cloudfour.storeservice.domain.menu.entity.Stock;
import com.example.cloudfour.storeservice.domain.menu.exception.MenuErrorCode;
import com.example.cloudfour.storeservice.domain.menu.exception.MenuException;
import com.example.cloudfour.storeservice.domain.menu.exception.StockErrorCode;
import com.example.cloudfour.storeservice.domain.menu.exception.StockException;
import com.example.cloudfour.storeservice.domain.menu.repository.MenuRepository;
import com.example.cloudfour.storeservice.domain.menu.repository.StockRepository;
import com.example.cloudfour.storeservice.domain.menu.service.RedisInventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class StockQueryService {
    private final StockRepository stockRepository;
    private final MenuRepository menuRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisInventoryService redisInventoryService;

    private static final String INVN_MENU_PREFIX = "invn:menu:";

    @Transactional(readOnly = true)
    public StockResponseDTO.StockCacheResponseDTO getMenuStock(UUID menuId){
        Menu menu = menuRepository.findById(menuId).orElseThrow(()->new MenuException(MenuErrorCode.NOT_FOUND));
        UUID stockId = menu.getStock().getId();

        String key = INVN_MENU_PREFIX + menuId;
        String cachedValue = null;
        try {
            cachedValue = redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Redis invn 조회 실패 - menuId: {}", menuId, e);
        }

        if (cachedValue != null) {
            Long qty = Long.valueOf(cachedValue);
            log.info("Redis(inv n)에서 재고 찾음 - menuId: {}, quantity: {}", menuId, qty);
            return StockConverter.CachetoStockResponseDTO(stockId,menuId,qty);
        }

        Stock stock = stockRepository.findByIdWithOptimisticLock(stockId).orElseThrow(()->new StockException(StockErrorCode.NOT_FOUND));
        log.info("Redis(inv n) 미스, DB에서 찾음 - stockId: {}, quantity: {}", stockId, stock.getQuantity());
        try {
            redisTemplate.opsForValue().set(key, String.valueOf(stock.getQuantity()));
        } catch (Exception e) {
            log.error("Redis invn 캐시 저장 실패 - menuId: {}", menuId, e);
        }
        return StockConverter.toStockResponseDTO(stock);
    }

    @Transactional(readOnly = true)
    public StockResponseDTO.StockAvailabilityResponseDTO getMenuStockAvailability(UUID menuId) {
        Menu menu = menuRepository.findById(menuId).orElseThrow(() -> new MenuException(MenuErrorCode.NOT_FOUND));
        UUID stockId = menu.getStock().getId();

        String key = INVN_MENU_PREFIX + menuId;
        Long baseQty = null;
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                baseQty = Long.valueOf(cached);
            }
        } catch (Exception e) {
            log.error("Redis invn 조회 실패 - menuId: {}", menuId, e);
        }
        if (baseQty == null) {
            Stock stock = stockRepository.findByIdWithOptimisticLock(stockId).orElseThrow(() -> new StockException(StockErrorCode.NOT_FOUND));
            baseQty = stock.getQuantity();
            try {
                redisTemplate.opsForValue().set(key, String.valueOf(baseQty));
            } catch (Exception e) {
                log.error("Redis invn 캐시 저장 실패 - menuId: {}", menuId, e);
            }
        }

        StockResponseDTO.StockAvailabilityResponseDTO avail = redisInventoryService.getAvailability(menuId);
        Long reserved = null;
        Long available = null;
        if (avail.getReservedQuantity() != null && avail.getAvailableQuantity() != null) {
            reserved = avail.getReservedQuantity();
            available = avail.getAvailableQuantity();
        } else {
            reserved = 0L;
            available = baseQty;
        }

        return StockResponseDTO.StockAvailabilityResponseDTO.builder()
                .stockId(stockId)
                .menuId(menuId)
                .baseQuantity(baseQty)
                .availableQuantity(available)
                .reservedQuantity(reserved)
                .build();
    }
}