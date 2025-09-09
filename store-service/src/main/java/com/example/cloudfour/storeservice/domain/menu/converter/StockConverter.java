package com.example.cloudfour.storeservice.domain.menu.converter;

import com.example.cloudfour.storeservice.domain.menu.dto.StockResponseDTO;
import com.example.cloudfour.storeservice.domain.menu.entity.Stock;
import com.example.cloudfour.storeservice.domain.menu.enums.ReservationStatus;

import java.util.UUID;

public class StockConverter {
    public static StockResponseDTO.StockCacheResponseDTO toStockResponseDTO(Stock stock){
        return StockResponseDTO.StockCacheResponseDTO.builder()
                .stockId(stock.getId())
                .menuId(stock.getMenu().getId())
                .quantity(stock.getQuantity())
                .build();
    }

    public static StockResponseDTO.StockCacheResponseDTO CachetoStockResponseDTO(UUID stockId,
           UUID menuId, Long quantity){
        return StockResponseDTO.StockCacheResponseDTO.builder()
                .stockId(stockId)
                .menuId(menuId)
                .quantity(quantity)
                .build();
    }

    public static StockResponseDTO.StockReserveResponseDTO success(Long remainingStock) {
        return StockResponseDTO.StockReserveResponseDTO.builder()
                .success(true)
                .message("Stock reserved successfully")
                .remainingStock(remainingStock)
                .status(ReservationStatus.RESERVED)
                .build();
    }

    public static StockResponseDTO.StockReserveResponseDTO insufficientStock(Long currentStock) {
        return StockResponseDTO.StockReserveResponseDTO.builder()
                .success(false)
                .message("Insufficient stock")
                .remainingStock(currentStock)
                .status(ReservationStatus.INSUFFICIENT_STOCK)
                .build();
    }

    public static StockResponseDTO.StockReserveResponseDTO menuNotFound() {
        return StockResponseDTO.StockReserveResponseDTO.builder()
                .success(false)
                .message("Menu not found")
                .status(ReservationStatus.MENU_NOT_FOUND)
                .build();
    }

    public static StockResponseDTO.StockReserveResponseDTO error(String message) {
        return StockResponseDTO.StockReserveResponseDTO.builder()
                .success(false)
                .message(message)
                .status(ReservationStatus.ERROR)
                .build();
    }
}
