package com.example.cloudfour.storeservice.domain.menu.service.query;

import com.example.cloudfour.storeservice.domain.commondto.MenuCartResponseDTO;
import com.example.cloudfour.storeservice.domain.menu.converter.MenuConverter;
import com.example.cloudfour.storeservice.domain.menu.entity.Menu;
import com.example.cloudfour.storeservice.domain.menu.exception.MenuErrorCode;
import com.example.cloudfour.storeservice.domain.menu.exception.MenuException;
import com.example.cloudfour.storeservice.domain.menu.repository.MenuRepository;
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
@DisplayName("MenuQueryService 단위테스트")
class MenuQueryServiceTest {

    @Mock
    private MenuRepository menuRepository;

    @InjectMocks
    private MenuQueryService menuQueryService;

    private UUID menuId;
    private Menu menu;
    private MenuCartResponseDTO menuCartResponseDTO;

    @BeforeEach
    void setUp() {
        menuId = UUID.randomUUID();
        
        menu = mock(Menu.class);
        lenient().when(menu.getId()).thenReturn(menuId);
        lenient().when(menu.getName()).thenReturn("Test Menu");
        lenient().when(menu.getPrice()).thenReturn(10000);
        
        menuCartResponseDTO = mock(MenuCartResponseDTO.class);
        lenient().when(menuCartResponseDTO.getMenuId()).thenReturn(menuId);
        lenient().when(menuCartResponseDTO.getPrice()).thenReturn(10000);
    }

    @Test
    @DisplayName("유효한 메뉴 ID가 주어지면 메뉴 정보를 반환한다")
    void findMenu_ValidMenuId_ReturnsMenuCart() {
        // Given
        when(menuRepository.findById(menuId)).thenReturn(Optional.of(menu));

        try (MockedStatic<MenuConverter> mockedStatic = mockStatic(MenuConverter.class)) {
            mockedStatic.when(() -> MenuConverter.toFindMenuDTO(menu))
                    .thenReturn(menuCartResponseDTO);

            // When
            MenuCartResponseDTO result = menuQueryService.findMenu(menuId);

            // Then
            assertThat(result).isEqualTo(menuCartResponseDTO);
            verify(menuRepository).findById(menuId);
            mockedStatic.verify(() -> MenuConverter.toFindMenuDTO(menu));
        }
    }

    @Test
    @DisplayName("존재하지 않는 메뉴 ID가 주어지면 예외를 던진다")
    void findMenu_MenuNotFound_ThrowsException() {
        // Given
        when(menuRepository.findById(menuId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> menuQueryService.findMenu(menuId))
                .isInstanceOf(MenuException.class)
                .hasFieldOrPropertyWithValue("code", MenuErrorCode.NOT_FOUND);

        verify(menuRepository).findById(menuId);
    }

    @Test
    @DisplayName("null 메뉴 ID가 주어지면 예외를 던진다")
    void findMenu_NullMenuId_ThrowsException() {
        // Given
        UUID nullMenuId = null;
        when(menuRepository.findById(nullMenuId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> menuQueryService.findMenu(nullMenuId))
                .isInstanceOf(MenuException.class)
                .hasFieldOrPropertyWithValue("code", MenuErrorCode.NOT_FOUND);

        verify(menuRepository).findById(nullMenuId);
    }
}