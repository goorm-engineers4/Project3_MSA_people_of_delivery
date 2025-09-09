package com.example.cloudfour.storeservice.domain.menu.dto;

import com.example.cloudfour.storeservice.domain.menu.enums.ReservationStatus;
import lombok.Builder;
import lombok.Getter;
import java.util.UUID;

public class StockResponseDTO {
    @Getter
    @Builder
    public static class StockCacheResponseDTO{
        private UUID stockId;
        private UUID menuId;
        private Long quantity;
    }

    @Getter
    @Builder
    public static class StockReserveResponseDTO{
        private final boolean success;
        private final String message;
        private final Long remainingStock;
        private final ReservationStatus status;
    }

    @Getter
    @Builder
    public static class StockUpdateInfo {
        private UUID stockId;
        private Long quantity;
    }

}
