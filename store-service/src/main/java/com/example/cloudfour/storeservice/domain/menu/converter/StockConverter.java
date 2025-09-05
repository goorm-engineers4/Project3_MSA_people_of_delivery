package com.example.cloudfour.storeservice.domain.menu.converter;

import com.example.cloudfour.storeservice.domain.menu.dto.StockResponseDTO;
import com.example.cloudfour.storeservice.domain.menu.entity.Stock;

import java.util.UUID;

public class StockConverter {
    public static StockResponseDTO toStockResposneDTO(Stock stock){
        return StockResponseDTO.builder()
                .stockId(stock.getId())
                .menuId(stock.getMenu().getId())
                .quantity(stock.getQuantity())
                .build();
    }

    public static StockResponseDTO CachetoStockResposneDTO(UUID stockId,
           UUID menuId, Long quantity){
        return StockResponseDTO.builder()
                .stockId(stockId)
                .menuId(menuId)
                .quantity(quantity)
                .build();
    }
}
