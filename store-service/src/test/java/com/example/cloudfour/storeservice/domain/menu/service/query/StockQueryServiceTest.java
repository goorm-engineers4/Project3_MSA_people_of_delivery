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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockQueryService 단위테스트")
class StockQueryServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private MenuRepository menuRepository;

    @InjectMocks
    private StockQueryService stockQueryService;

    private UUID menuId;
    private UUID stockId;
    private Menu menu;
    private Stock stock;
    private StockResponseDTO stockResponseDTO;

    @BeforeEach
    void setUp() {
        menuId = UUID.randomUUID();
        stockId = UUID.randomUUID();
        
        stock = mock(Stock.class);
        lenient().when(stock.getId()).thenReturn(stockId);
        lenient().when(stock.getQuantity()).thenReturn(100L);
        lenient().when(stock.getVersion()).thenReturn(1L);
        
        menu = mock(Menu.class);
        lenient().when(menu.getId()).thenReturn(menuId);
        lenient().when(menu.getName()).thenReturn("Test Menu");
        lenient().when(menu.getStock()).thenReturn(stock);
        
        stockResponseDTO = StockResponseDTO.builder()
                .stockId(stockId)
                .menuId(menuId)
                .quantity(100L)
                .version(1L)
                .build();
    }

    @Test
    @DisplayName("유효한 메뉴 ID가 주어지면 재고 정보를 반환한다")
    void getMenuStock_ValidMenuId_ReturnsStock() {
        // Given
        when(menuRepository.findById(menuId)).thenReturn(Optional.of(menu));
        when(stockRepository.findByIdWithOptimisticLock(stockId)).thenReturn(Optional.of(stock));

        try (MockedStatic<StockConverter> mockedStatic = mockStatic(StockConverter.class)) {
            mockedStatic.when(() -> StockConverter.toStockResposneDTO(stock))
                    .thenReturn(stockResponseDTO);

            // When
            StockResponseDTO result = stockQueryService.getMenuStock(menuId);

            // Then
            assertThat(result).isEqualTo(stockResponseDTO);
            assertThat(result.getStockId()).isEqualTo(stockId);
            assertThat(result.getMenuId()).isEqualTo(menuId);
            assertThat(result.getQuantity()).isEqualTo(100L);
            assertThat(result.getVersion()).isEqualTo(1L);

            verify(menuRepository).findById(menuId);
            verify(stockRepository).findByIdWithOptimisticLock(stockId);
            mockedStatic.verify(() -> StockConverter.toStockResposneDTO(stock));
        }
    }

    @Test
    @DisplayName("존재하지 않는 메뉴 ID가 주어지면 예외를 던진다")
    void getMenuStock_MenuNotFound_ThrowsException() {
        // Given
        when(menuRepository.findById(menuId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> stockQueryService.getMenuStock(menuId))
                .isInstanceOf(MenuException.class)
                .hasFieldOrPropertyWithValue("code", MenuErrorCode.NOT_FOUND);

        verify(menuRepository).findById(menuId);
        verifyNoInteractions(stockRepository);
    }

    @Test
    @DisplayName("메뉴는 존재하지만 재고가 없으면 예외를 던진다")
    void getMenuStock_StockNotFound_ThrowsException() {
        // Given
        when(menuRepository.findById(menuId)).thenReturn(Optional.of(menu));
        when(stockRepository.findByIdWithOptimisticLock(stockId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> stockQueryService.getMenuStock(menuId))
                .isInstanceOf(StockException.class)
                .hasFieldOrPropertyWithValue("code", StockErrorCode.NOT_FOUND);

        verify(menuRepository).findById(menuId);
        verify(stockRepository).findByIdWithOptimisticLock(stockId);
    }

    @Test
    @DisplayName("null 메뉴 ID가 주어지면 예외를 던진다")
    void getMenuStock_NullMenuId_ThrowsException() {
        // Given
        UUID nullMenuId = null;
        when(menuRepository.findById(nullMenuId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> stockQueryService.getMenuStock(nullMenuId))
                .isInstanceOf(MenuException.class)
                .hasFieldOrPropertyWithValue("code", MenuErrorCode.NOT_FOUND);

        verify(menuRepository).findById(nullMenuId);
    }

    @Test
    @DisplayName("StockConverter가 null을 반환해도 정상적으로 처리한다")
    void getMenuStock_ConverterReturnsNull_ReturnsNull() {
        // Given
        when(menuRepository.findById(menuId)).thenReturn(Optional.of(menu));
        when(stockRepository.findByIdWithOptimisticLock(stockId)).thenReturn(Optional.of(stock));

        try (MockedStatic<StockConverter> mockedStatic = mockStatic(StockConverter.class)) {
            mockedStatic.when(() -> StockConverter.toStockResposneDTO(stock))
                    .thenReturn(null);

            // When
            StockResponseDTO result = stockQueryService.getMenuStock(menuId);

            // Then
            assertThat(result).isNull();

            verify(menuRepository).findById(menuId);
            verify(stockRepository).findByIdWithOptimisticLock(stockId);
            mockedStatic.verify(() -> StockConverter.toStockResposneDTO(stock));
        }
    }

    @Test
    @DisplayName("낙관적 락을 사용하여 재고를 조회한다")
    void getMenuStock_UsesOptimisticLock() {
        // Given
        when(menuRepository.findById(menuId)).thenReturn(Optional.of(menu));
        when(stockRepository.findByIdWithOptimisticLock(stockId)).thenReturn(Optional.of(stock));

        try (MockedStatic<StockConverter> mockedStatic = mockStatic(StockConverter.class)) {
            mockedStatic.when(() -> StockConverter.toStockResposneDTO(stock))
                    .thenReturn(stockResponseDTO);

            // When
            stockQueryService.getMenuStock(menuId);

            // Then
            // findByIdWithOptimisticLock 메서드가 호출되었는지 확인
            verify(stockRepository).findByIdWithOptimisticLock(stockId);
            verify(stockRepository, never()).findById(any(UUID.class)); // 일반 findById는 호출되지 않음
        }
    }
}