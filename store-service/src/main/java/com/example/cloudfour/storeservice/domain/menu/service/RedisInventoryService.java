package com.example.cloudfour.storeservice.domain.menu.service;

import com.example.cloudfour.storeservice.domain.menu.converter.StockConverter;
import com.example.cloudfour.storeservice.domain.menu.dto.StockResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class RedisInventoryService {

    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<List> reserveStockScript;
    private final DefaultRedisScript<List> releaseStockScript;

    private static final String STOCK_KEY_PREFIX = "invn:menu:";
    private static final String RESERVATION_KEY_PREFIX = "rsrv:menu:";
    private static final String ORDER_RESERVATION_KEY_PREFIX = "order:rsrv:";

    public RedisInventoryService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.reserveStockScript = new DefaultRedisScript<>();
        this.reserveStockScript.setScriptText(getReserveStockScript());
        this.reserveStockScript.setResultType(List.class);

        this.releaseStockScript = new DefaultRedisScript<>();
        this.releaseStockScript.setScriptText(getReleaseStockScript());
        this.releaseStockScript.setResultType(List.class);
    }

    public StockResponseDTO.StockReserveResponseDTO reserveStock(UUID menuId, Long quantity, UUID orderId) {
        String stockKey = STOCK_KEY_PREFIX + menuId;
        String reservationKey = RESERVATION_KEY_PREFIX + menuId;
        String orderReservationKey = ORDER_RESERVATION_KEY_PREFIX + orderId;

        try {
            @SuppressWarnings("unchecked")
            List<Object> result = redisTemplate.execute(
                    reserveStockScript,
                    Arrays.asList(stockKey, reservationKey, orderReservationKey),
                    String.valueOf(quantity),
                    orderId.toString(),
                    String.valueOf(System.currentTimeMillis() + 300000)
            );

            int code = ((Number) result.get(0)).intValue();
            String message = (String) result.get(1);

            switch (code) {
                case 0:
                    Long remainingStock = ((Number) result.get(2)).longValue();
                    log.info("재고 예약 성공 - MenuId: {}, Quantity: {}, Remaining: {}, OrderId: {}",
                            menuId, quantity, remainingStock, orderId);
                    return StockConverter.success(remainingStock);

                case -1:
                    log.warn("메뉴를 찾을 수 없음 - MenuId: {}", menuId);
                    return StockConverter.menuNotFound();

                case -2:
                    Long currentStock = ((Number) result.get(2)).longValue();
                    log.warn("재고 부족 - MenuId: {}, Required: {}, Available: {}",
                            menuId, quantity, currentStock);
                    return StockConverter.insufficientStock(currentStock);

                default:
                    log.error("Internal Error - Code: {}, Message: {}", code, message);
                    return StockConverter.error(message);
            }
        } catch (Exception e) {
            log.error("Redis 재고 예약 중 오류 발생 - MenuId: {}, OrderId: {}", menuId, orderId, e);
            return StockConverter.error("Redis operation failed");
        }
    }

    public void releaseReservation(UUID orderId) {
        String orderReservationKey = ORDER_RESERVATION_KEY_PREFIX + orderId;

        try {
            Map<Object, Object> reservation = redisTemplate.opsForHash().entries(orderReservationKey);
            if (reservation.isEmpty()) {
                log.warn("주문ID에 대한 예약이 없음: {}", orderId);
                return;
            }

            String menuIdStr = (String) reservation.get("menuId");
            String quantityStr = (String) reservation.get("quantity");
            
            if (menuIdStr == null || quantityStr == null) {
                log.error("잘못된 예약 데이터 - OrderId: {}", orderId);
                return;
            }

            UUID menuId = UUID.fromString(menuIdStr);
            Long quantity = Long.valueOf(quantityStr);

            String stockKey = STOCK_KEY_PREFIX + menuId;
            String reservationKey = RESERVATION_KEY_PREFIX + menuId;

            @SuppressWarnings("unchecked")
            List<Object> result = redisTemplate.execute(
                    releaseStockScript,
                    Arrays.asList(stockKey, reservationKey, orderReservationKey),
                    quantity.toString(),
                    orderId.toString()
            );

            int code = ((Number) result.get(0)).intValue();
            if (code == 0) {
                log.info("재고 예약 해제 성공 - OrderId: {}, MenuId: {}, Quantity: {}",
                        orderId, menuId, quantity);
            } else {
                log.error("재고 예약 해제 실패 - OrderId: {}, Code: {}", orderId, code);
            }
        } catch (Exception e) {
            log.error("예약 해제 중 오류 발생 - OrderId: {}", orderId, e);
        }
    }

    private String getReserveStockScript() {
        return """
            local stockKey = KEYS[1]
            local reservationKey = KEYS[2]
            local orderReservationKey = KEYS[3]
            local quantity = tonumber(ARGV[1])
            local orderId = ARGV[2]
            local expireTime = ARGV[3]
        
            -- 현재 재고 확인
            local currentStock = redis.call('GET', stockKey)
            if currentStock == false then
                return {-1, "MENU_NOT_FOUND"}
            end
        
            currentStock = tonumber(currentStock)
        
            -- 현재 예약 수량 확인
            local reservedStock = 0
            local reservations = redis.call('HGETALL', reservationKey)
            for i = 2, #reservations, 2 do
                reservedStock = reservedStock + tonumber(reservations[i])
            end
        
            -- 사용 가능한 재고 계산
            local availableStock = currentStock - reservedStock
            
            if availableStock < quantity then
                return {-2, "INSUFFICIENT_STOCK", availableStock}
            end
            
            -- 예약 생성
            redis.call('HSET', reservationKey, orderId, quantity)
            redis.call('EXPIRE', reservationKey, 7200)
            
            -- menuId를 stockKey에서 추출 (수정된 prefix 반영)
            local menuId = string.match(stockKey, 'invn:menu:(.+)')
            
            -- 주문별 예약 정보 저장
            redis.call('HMSET', orderReservationKey, 
                'menuId', menuId,
                'quantity', quantity,
                'expireTime', expireTime)
            redis.call('EXPIRE', orderReservationKey, 300)
            
            local newAvailableStock = availableStock - quantity
            return {0, "SUCCESS", newAvailableStock}
        """;
    }

    private String getReleaseStockScript() {
        return """
            local stockKey = KEYS[1]
            local reservationKey = KEYS[2]
            local orderReservationKey = KEYS[3]
            local quantity = tonumber(ARGV[1])
            local orderId = ARGV[2]
            
            -- 예약 확인 및 해제
            local reservedQuantity = redis.call('HGET', reservationKey, orderId)
            if reservedQuantity == false then
                return {-1, "RESERVATION_NOT_FOUND"}
            end
            
            if tonumber(reservedQuantity) ~= quantity then
                return {-2, "QUANTITY_MISMATCH"}
            end
            
            -- 예약 해제
            redis.call('HDEL', reservationKey, orderId)
            redis.call('DEL', orderReservationKey)
            
            return {0, "SUCCESS"}
        """;
    }
}