package com.example.cloudfour.storeservice.domain.menu.service.command;

import com.example.cloudfour.modulecommon.dto.CurrentUser;
import com.example.cloudfour.storeservice.domain.menu.converter.MenuConverter;
import com.example.cloudfour.storeservice.domain.menu.converter.MenuOptionConverter;
import com.example.cloudfour.storeservice.domain.menu.controller.MenuCommonRequestDTO;
import com.example.cloudfour.storeservice.domain.menu.dto.MenuRequestDTO;
import com.example.cloudfour.storeservice.domain.menu.dto.MenuResponseDTO;
import com.example.cloudfour.storeservice.domain.menu.dto.MenuOptionResponseDTO;
import com.example.cloudfour.storeservice.domain.menu.entity.Menu;
import com.example.cloudfour.storeservice.domain.menu.entity.MenuCategory;
import com.example.cloudfour.storeservice.domain.menu.entity.MenuOption;
import com.example.cloudfour.storeservice.domain.menu.entity.Stock;
import com.example.cloudfour.storeservice.domain.menu.enums.MenuStatus;
import com.example.cloudfour.storeservice.domain.menu.exception.MenuErrorCode;
import com.example.cloudfour.storeservice.domain.menu.exception.MenuException;
import com.example.cloudfour.storeservice.domain.menu.exception.MenuOptionErrorCode;
import com.example.cloudfour.storeservice.domain.menu.exception.MenuOptionException;
import com.example.cloudfour.storeservice.domain.menu.repository.MenuCategoryRepository;
import com.example.cloudfour.storeservice.domain.menu.repository.MenuOptionRepository;
import com.example.cloudfour.storeservice.domain.menu.repository.MenuRepository;
import com.example.cloudfour.storeservice.domain.store.entity.Store;
import com.example.cloudfour.storeservice.domain.store.exception.StoreErrorCode;
import com.example.cloudfour.storeservice.domain.store.exception.StoreException;
import com.example.cloudfour.storeservice.domain.store.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MenuCommandService 단위테스트")
class MenuCommandServiceTest {

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private MenuCategoryRepository menuCategoryRepository;

    @Mock
    private MenuOptionRepository menuOptionRepository;

    @InjectMocks
    private MenuCommandService menuCommandService;

    private UUID userId;
    private UUID storeId;
    private UUID menuId;
    private UUID optionId;
    private CurrentUser currentUser;
    private Store store;
    private Menu menu;
    private MenuCategory menuCategory;
    private MenuOption menuOption;
    private MenuRequestDTO.MenuCreateRequestDTO createRequestDTO;
    private MenuRequestDTO.MenuUpdateRequestDTO updateRequestDTO;
    private MenuRequestDTO.MenuOptionCreateRequestDTO optionCreateRequestDTO;
    private MenuRequestDTO.MenuOptionUpdateRequestDTO optionUpdateRequestDTO;
    private MenuResponseDTO.MenuDetailResponseDTO menuDetailResponseDTO;
    private MenuOptionResponseDTO.MenuOptionSimpleResponseDTO menuOptionSimpleResponseDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        menuId = UUID.randomUUID();
        optionId = UUID.randomUUID();
        currentUser = new CurrentUser(userId, "ROLE_OWNER");

        // Mock objects setup
        store = mock(Store.class);
        lenient().when(store.getId()).thenReturn(storeId);
        lenient().when(store.getOwnerId()).thenReturn(userId);

        menuCategory = mock(MenuCategory.class);
        lenient().when(menuCategory.getCategory()).thenReturn("Test Category");

        menu = mock(Menu.class);
        lenient().when(menu.getId()).thenReturn(menuId);
        lenient().when(menu.getName()).thenReturn("Test Menu");
        lenient().when(menu.getStore()).thenReturn(store);

        menuOption = mock(MenuOption.class);
        lenient().when(menuOption.getId()).thenReturn(optionId);
        lenient().when(menuOption.getOptionName()).thenReturn("Test Option");
        lenient().when(menuOption.getMenu()).thenReturn(menu);

        // DTO setup
        MenuCommonRequestDTO.MenuCommonMainRequestDTO menuCommonMainRequestDTO = 
            MenuCommonRequestDTO.MenuCommonMainRequestDTO.builder()
                .name("Test Menu")
                .content("Test Content")
                .price(10000)
                .menuPicture("test.jpg")
                .status(MenuStatus.판매중)
                .category("Test Category")
                .build();

        createRequestDTO = MenuRequestDTO.MenuCreateRequestDTO.builder()
                .menuCommonMainRequestDTO(menuCommonMainRequestDTO)
                .quantity(100L)
                .build();

        updateRequestDTO = MenuRequestDTO.MenuUpdateRequestDTO.builder()
                .menuCommonMainRequestDTO(menuCommonMainRequestDTO)
                .build();

        MenuCommonRequestDTO.MenuOptionCommonRequestDTO menuOptionCommonRequestDTO = 
            MenuCommonRequestDTO.MenuOptionCommonRequestDTO.builder()
                .optionName("Test Option")
                .additionalPrice(2000)
                .build();

        optionCreateRequestDTO = MenuRequestDTO.MenuOptionCreateRequestDTO.builder()
                .menuOptionCommonRequestDTO(menuOptionCommonRequestDTO)
                .build();

        optionUpdateRequestDTO = MenuRequestDTO.MenuOptionUpdateRequestDTO.builder()
                .menuOptionCommonRequestDTO(menuOptionCommonRequestDTO)
                .build();

        // Response DTOs
        menuDetailResponseDTO = mock(MenuResponseDTO.MenuDetailResponseDTO.class);
        menuOptionSimpleResponseDTO = mock(MenuOptionResponseDTO.MenuOptionSimpleResponseDTO.class);
    }

    @Nested
    @DisplayName("createMenu 메서드는")
    class CreateMenuTests {

        @Test
        @DisplayName("유효한 요청이 주어지면 메뉴를 생성한다")
        void createMenu_ValidRequest_ReturnsMenuDetail() {
            // Given
            when(storeRepository.findByIdAndIsDeletedFalse(storeId)).thenReturn(Optional.of(store));
            when(menuCategoryRepository.findByCategory("Test Category")).thenReturn(Optional.of(menuCategory));
            when(menuRepository.existsByNameAndStoreId("Test Menu", storeId)).thenReturn(false);
            when(menuRepository.save(any(Menu.class))).thenReturn(menu);

            try (MockedStatic<MenuConverter> mockedStatic = mockStatic(MenuConverter.class)) {
                mockedStatic.when(() -> MenuConverter.toMenu(createRequestDTO)).thenReturn(menu);
                mockedStatic.when(() -> MenuConverter.toMenuDetail1ResponseDTO(menu))
                        .thenReturn(menuDetailResponseDTO);

                // When
                MenuResponseDTO.MenuDetailResponseDTO result = 
                    menuCommandService.createMenu(createRequestDTO, storeId, currentUser);

                // Then
                assertThat(result).isEqualTo(menuDetailResponseDTO);
                verify(storeRepository).findByIdAndIsDeletedFalse(storeId);
                verify(menuRepository).existsByNameAndStoreId("Test Menu", storeId);
                verify(menuRepository).save(any(Menu.class));
            }
        }

        @Test
        @DisplayName("존재하지 않는 가게면 예외를 던진다")
        void createMenu_StoreNotFound_ThrowsException() {
            // Given
            when(storeRepository.findByIdAndIsDeletedFalse(storeId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> menuCommandService.createMenu(createRequestDTO, storeId, currentUser))
                    .isInstanceOf(StoreException.class)
                    .hasFieldOrPropertyWithValue("code", StoreErrorCode.NOT_FOUND);
        }

        @Test
        @DisplayName("사용자가 null이면 예외를 던진다")
        void createMenu_NullUser_ThrowsException() {
            // Given
            when(storeRepository.findByIdAndIsDeletedFalse(storeId)).thenReturn(Optional.of(store));

            // When & Then
            assertThatThrownBy(() -> menuCommandService.createMenu(createRequestDTO, storeId, null))
                    .isInstanceOf(MenuException.class)
                    .hasFieldOrPropertyWithValue("code", MenuErrorCode.UNAUTHORIZED_ACCESS);
        }

        @Test
        @DisplayName("가게 소유자가 아니면 예외를 던진다")
        void createMenu_UnauthorizedUser_ThrowsException() {
            // Given
            CurrentUser unauthorizedUser = new CurrentUser(UUID.randomUUID(), "ROLE_OWNER");
            when(storeRepository.findByIdAndIsDeletedFalse(storeId)).thenReturn(Optional.of(store));

            // When & Then
            assertThatThrownBy(() -> menuCommandService.createMenu(createRequestDTO, storeId, unauthorizedUser))
                    .isInstanceOf(MenuException.class)
                    .hasFieldOrPropertyWithValue("code", MenuErrorCode.UNAUTHORIZED_ACCESS);
        }

        @Test
        @DisplayName("이미 존재하는 메뉴 이름이면 예외를 던진다")
        void createMenu_DuplicateMenuName_ThrowsException() {
            // Given
            when(storeRepository.findByIdAndIsDeletedFalse(storeId)).thenReturn(Optional.of(store));
            when(menuCategoryRepository.findByCategory("Test Category")).thenReturn(Optional.of(menuCategory));
            when(menuRepository.existsByNameAndStoreId("Test Menu", storeId)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> menuCommandService.createMenu(createRequestDTO, storeId, currentUser))
                    .isInstanceOf(MenuException.class)
                    .hasFieldOrPropertyWithValue("code", MenuErrorCode.ALREADY_ADD);
        }

        @Test
        @DisplayName("새로운 카테고리면 카테고리를 생성한다")
        void createMenu_NewCategory_CreatesCategory() {
            // Given
            when(storeRepository.findByIdAndIsDeletedFalse(storeId)).thenReturn(Optional.of(store));
            when(menuCategoryRepository.findByCategory("Test Category")).thenReturn(Optional.empty());
            when(menuCategoryRepository.save(any(MenuCategory.class))).thenReturn(menuCategory);
            when(menuRepository.existsByNameAndStoreId("Test Menu", storeId)).thenReturn(false);
            when(menuRepository.save(any(Menu.class))).thenReturn(menu);

            try (MockedStatic<MenuConverter> mockedStatic = mockStatic(MenuConverter.class)) {
                mockedStatic.when(() -> MenuConverter.toMenu(createRequestDTO)).thenReturn(menu);
                mockedStatic.when(() -> MenuConverter.toMenuDetail1ResponseDTO(menu))
                        .thenReturn(menuDetailResponseDTO);

                // When
                menuCommandService.createMenu(createRequestDTO, storeId, currentUser);

                // Then
                verify(menuCategoryRepository).save(any(MenuCategory.class));
            }
        }
    }

    @Nested
    @DisplayName("updateMenu 메서드는")
    class UpdateMenuTests {

        @Test
        @DisplayName("유효한 요청이 주어지면 메뉴를 수정한다")
        void updateMenu_ValidRequest_ReturnsUpdatedMenu() {
            // Given
            when(menuRepository.findById(menuId)).thenReturn(Optional.of(menu));
            when(menuCategoryRepository.findByCategory("Test Category")).thenReturn(Optional.of(menuCategory));
            when(menuRepository.save(menu)).thenReturn(menu);

            try (MockedStatic<MenuConverter> mockedStatic = mockStatic(MenuConverter.class)) {
                mockedStatic.when(() -> MenuConverter.toMenuDetail1ResponseDTO(menu))
                        .thenReturn(menuDetailResponseDTO);

                // When
                MenuResponseDTO.MenuDetailResponseDTO result = 
                    menuCommandService.updateMenu(menuId, updateRequestDTO, currentUser);

                // Then
                assertThat(result).isEqualTo(menuDetailResponseDTO);
                verify(menuRepository).findById(menuId);
                verify(menu).updateMenuInfo(any(), any(), any(), any(), any());
                verify(menuRepository).save(menu);
            }
        }

        @Test
        @DisplayName("존재하지 않는 메뉴면 예외를 던진다")
        void updateMenu_MenuNotFound_ThrowsException() {
            // Given
            when(menuRepository.findById(menuId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> menuCommandService.updateMenu(menuId, updateRequestDTO, currentUser))
                    .isInstanceOf(MenuException.class)
                    .hasFieldOrPropertyWithValue("code", MenuErrorCode.NOT_FOUND);
        }

        @Test
        @DisplayName("사용자가 null이면 예외를 던진다")
        void updateMenu_NullUser_ThrowsException() {
            // Given
            when(menuRepository.findById(menuId)).thenReturn(Optional.of(menu));

            // When & Then
            assertThatThrownBy(() -> menuCommandService.updateMenu(menuId, updateRequestDTO, null))
                    .isInstanceOf(MenuException.class)
                    .hasFieldOrPropertyWithValue("code", MenuErrorCode.UNAUTHORIZED_ACCESS);
        }

        @Test
        @DisplayName("메뉴 소유자가 아니면 예외를 던진다")
        void updateMenu_UnauthorizedUser_ThrowsException() {
            // Given
            CurrentUser unauthorizedUser = new CurrentUser(UUID.randomUUID(), "ROLE_OWNER");
            when(menuRepository.findById(menuId)).thenReturn(Optional.of(menu));

            // When & Then
            assertThatThrownBy(() -> menuCommandService.updateMenu(menuId, updateRequestDTO, unauthorizedUser))
                    .isInstanceOf(MenuException.class)
                    .hasFieldOrPropertyWithValue("code", MenuErrorCode.UNAUTHORIZED_ACCESS);
        }
    }

    @Nested
    @DisplayName("deleteMenu 메서드는")
    class DeleteMenuTests {

        @Test
        @DisplayName("유효한 요청이 주어지면 메뉴를 삭제한다")
        void deleteMenu_ValidRequest_DeletesMenu() {
            // Given
            when(menuRepository.findById(menuId)).thenReturn(Optional.of(menu));

            // When
            menuCommandService.deleteMenu(menuId, currentUser);

            // Then
            verify(menuRepository).findById(menuId);
            verify(menu).softDelete();
        }

        @Test
        @DisplayName("존재하지 않는 메뉴면 예외를 던진다")
        void deleteMenu_MenuNotFound_ThrowsException() {
            // Given
            when(menuRepository.findById(menuId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> menuCommandService.deleteMenu(menuId, currentUser))
                    .isInstanceOf(MenuException.class)
                    .hasFieldOrPropertyWithValue("code", MenuErrorCode.NOT_FOUND);
        }

        @Test
        @DisplayName("사용자가 null이면 예외를 던진다")
        void deleteMenu_NullUser_ThrowsException() {
            // Given
            when(menuRepository.findById(menuId)).thenReturn(Optional.of(menu));

            // When & Then
            assertThatThrownBy(() -> menuCommandService.deleteMenu(menuId, null))
                    .isInstanceOf(MenuException.class)
                    .hasFieldOrPropertyWithValue("code", MenuErrorCode.UNAUTHORIZED_ACCESS);
        }
    }

    @Nested
    @DisplayName("createMenuOption 메서드는")
    class CreateMenuOptionTests {

        @Test
        @DisplayName("유효한 요청이 주어지면 메뉴 옵션을 생성한다")
        void createMenuOption_ValidRequest_ReturnsMenuOption() {
            // Given
            when(menuRepository.findById(menuId)).thenReturn(Optional.of(menu));
            when(menuOptionRepository.existsByMenuIdAndOptionName(menuId, "Test Option")).thenReturn(false);
            when(menuOptionRepository.save(any(MenuOption.class))).thenReturn(menuOption);

            try (MockedStatic<MenuOptionConverter> mockedStatic = mockStatic(MenuOptionConverter.class)) {
                mockedStatic.when(() -> MenuOptionConverter.toMenuOptionSimpleResponseDTO(any(MenuOption.class)))
                        .thenReturn(menuOptionSimpleResponseDTO);

                // When
                MenuOptionResponseDTO.MenuOptionSimpleResponseDTO result = 
                    menuCommandService.createMenuOption(optionCreateRequestDTO, currentUser, menuId);

                // Then
                assertThat(result).isEqualTo(menuOptionSimpleResponseDTO);
                verify(menuRepository).findById(menuId);
                verify(menuOptionRepository).save(any(MenuOption.class));
                mockedStatic.verify(() -> MenuOptionConverter.toMenuOptionSimpleResponseDTO(any(MenuOption.class)));
            }
        }

        @Test
        @DisplayName("존재하지 않는 메뉴면 예외를 던진다")
        void createMenuOption_MenuNotFound_ThrowsException() {
            // Given
            when(menuRepository.findById(menuId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> menuCommandService.createMenuOption(optionCreateRequestDTO, currentUser, menuId))
                    .isInstanceOf(MenuException.class)
                    .hasFieldOrPropertyWithValue("code", MenuErrorCode.NOT_FOUND);
        }

        @Test
        @DisplayName("사용자가 null이면 예외를 던진다")
        void createMenuOption_NullUser_ThrowsException() {
            // Given
            when(menuRepository.findById(menuId)).thenReturn(Optional.of(menu));

            // When & Then
            assertThatThrownBy(() -> menuCommandService.createMenuOption(optionCreateRequestDTO, null, menuId))
                    .isInstanceOf(MenuOptionException.class)
                    .hasFieldOrPropertyWithValue("code", MenuOptionErrorCode.UNAUTHORIZED_ACCESS);
        }

        @Test
        @DisplayName("가게 소유자가 아니면 예외를 던진다")
        void createMenuOption_UnauthorizedUser_ThrowsException() {
            // Given
            CurrentUser unauthorizedUser = new CurrentUser(UUID.randomUUID(), "ROLE_OWNER");
            when(menuRepository.findById(menuId)).thenReturn(Optional.of(menu));

            // When & Then
            assertThatThrownBy(() -> menuCommandService.createMenuOption(optionCreateRequestDTO, unauthorizedUser, menuId))
                    .isInstanceOf(MenuOptionException.class)
                    .hasFieldOrPropertyWithValue("code", MenuOptionErrorCode.UNAUTHORIZED_ACCESS);
        }

        @Test
        @DisplayName("이미 존재하는 옵션 이름이면 예외를 던진다")
        void createMenuOption_DuplicateOptionName_ThrowsException() {
            // Given
            when(menuRepository.findById(menuId)).thenReturn(Optional.of(menu));
            when(menuOptionRepository.existsByMenuIdAndOptionName(menuId, "Test Option")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> menuCommandService.createMenuOption(optionCreateRequestDTO, currentUser, menuId))
                    .isInstanceOf(MenuOptionException.class)
                    .hasFieldOrPropertyWithValue("code", MenuOptionErrorCode.ALREADY_ADD);
        }
    }

    @Nested
    @DisplayName("updateMenuOption 메서드는")
    class UpdateMenuOptionTests {

        @Test
        @DisplayName("유효한 요청이 주어지면 메뉴 옵션을 수정한다")
        void updateMenuOption_ValidRequest_ReturnsUpdatedOption() {
            // Given
            when(menuOptionRepository.findByIdWithMenu(optionId)).thenReturn(Optional.of(menuOption));
            // 옵션 이름이 동일하므로 중복 검사를 하지 않음 (실제 서비스 로직과 맞춤)
            when(menuOptionRepository.save(menuOption)).thenReturn(menuOption);

            try (MockedStatic<MenuOptionConverter> mockedStatic = mockStatic(MenuOptionConverter.class)) {
                mockedStatic.when(() -> MenuOptionConverter.toMenuOptionSimpleResponseDTO(menuOption))
                        .thenReturn(menuOptionSimpleResponseDTO);

                // When
                MenuOptionResponseDTO.MenuOptionSimpleResponseDTO result = 
                    menuCommandService.updateMenuOption(optionId, optionUpdateRequestDTO, currentUser);

                // Then
                assertThat(result).isEqualTo(menuOptionSimpleResponseDTO);
                verify(menuOptionRepository).findByIdWithMenu(optionId);
                verify(menuOption).updateOptionInfo("Test Option", 2000);
                verify(menuOptionRepository).save(menuOption);
            }
        }

        @Test
        @DisplayName("존재하지 않는 메뉴 옵션이면 예외를 던진다")
        void updateMenuOption_OptionNotFound_ThrowsException() {
            // Given
            when(menuOptionRepository.findByIdWithMenu(optionId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> menuCommandService.updateMenuOption(optionId, optionUpdateRequestDTO, currentUser))
                    .isInstanceOf(MenuOptionException.class)
                    .hasFieldOrPropertyWithValue("code", MenuOptionErrorCode.NOT_FOUND);
        }

        @Test
        @DisplayName("사용자가 null이면 예외를 던진다")
        void updateMenuOption_NullUser_ThrowsException() {
            // Given
            when(menuOptionRepository.findByIdWithMenu(optionId)).thenReturn(Optional.of(menuOption));

            // When & Then
            assertThatThrownBy(() -> menuCommandService.updateMenuOption(optionId, optionUpdateRequestDTO, null))
                    .isInstanceOf(MenuOptionException.class)
                    .hasFieldOrPropertyWithValue("code", MenuOptionErrorCode.UNAUTHORIZED_ACCESS);
        }

        @Test
        @DisplayName("가게 소유자가 아니면 예외를 던진다")
        void updateMenuOption_UnauthorizedUser_ThrowsException() {
            // Given
            CurrentUser unauthorizedUser = new CurrentUser(UUID.randomUUID(), "ROLE_OWNER");
            when(menuOptionRepository.findByIdWithMenu(optionId)).thenReturn(Optional.of(menuOption));

            // When & Then
            assertThatThrownBy(() -> menuCommandService.updateMenuOption(optionId, optionUpdateRequestDTO, unauthorizedUser))
                    .isInstanceOf(MenuOptionException.class)
                    .hasFieldOrPropertyWithValue("code", MenuOptionErrorCode.UNAUTHORIZED_ACCESS);
        }

        @Test
        @DisplayName("다른 이름으로 변경 시 이미 존재하는 옵션 이름이면 예외를 던진다")
        void updateMenuOption_DuplicateOptionNameWhenChanging_ThrowsException() {
            // Given
            MenuCommonRequestDTO.MenuOptionCommonRequestDTO newOptionRequestDTO = 
                MenuCommonRequestDTO.MenuOptionCommonRequestDTO.builder()
                    .optionName("Different Option")  // 다른 이름으로 변경
                    .additionalPrice(2000)
                    .build();

            MenuRequestDTO.MenuOptionUpdateRequestDTO newUpdateRequestDTO = 
                MenuRequestDTO.MenuOptionUpdateRequestDTO.builder()
                    .menuOptionCommonRequestDTO(newOptionRequestDTO)
                    .build();

            when(menuOptionRepository.findByIdWithMenu(optionId)).thenReturn(Optional.of(menuOption));
            when(menuOptionRepository.existsByMenuIdAndOptionName(menuId, "Different Option")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> menuCommandService.updateMenuOption(optionId, newUpdateRequestDTO, currentUser))
                    .isInstanceOf(MenuOptionException.class)
                    .hasFieldOrPropertyWithValue("code", MenuOptionErrorCode.ALREADY_ADD);
        }
    }

    @Nested
    @DisplayName("deleteMenuOption 메서드는")
    class DeleteMenuOptionTests {

        @Test
        @DisplayName("유효한 요청이 주어지면 메뉴 옵션을 삭제한다")
        void deleteMenuOption_ValidRequest_DeletesOption() {
            // Given
            when(menuOptionRepository.findByIdWithMenu(optionId)).thenReturn(Optional.of(menuOption));

            // When
            menuCommandService.deleteMenuOption(optionId, currentUser);

            // Then
            verify(menuOptionRepository).findByIdWithMenu(optionId);
            verify(menuOption).softDelete();
        }

        @Test
        @DisplayName("존재하지 않는 메뉴 옵션이면 예외를 던진다")
        void deleteMenuOption_OptionNotFound_ThrowsException() {
            // Given
            when(menuOptionRepository.findByIdWithMenu(optionId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> menuCommandService.deleteMenuOption(optionId, currentUser))
                    .isInstanceOf(MenuOptionException.class)
                    .hasFieldOrPropertyWithValue("code", MenuOptionErrorCode.NOT_FOUND);
        }

        @Test
        @DisplayName("사용자가 null이면 예외를 던진다")
        void deleteMenuOption_NullUser_ThrowsException() {
            // Given
            when(menuOptionRepository.findByIdWithMenu(optionId)).thenReturn(Optional.of(menuOption));

            // When & Then
            assertThatThrownBy(() -> menuCommandService.deleteMenuOption(optionId, null))
                    .isInstanceOf(MenuException.class)
                    .hasFieldOrPropertyWithValue("code", MenuErrorCode.UNAUTHORIZED_ACCESS);
        }

        @Test
        @DisplayName("가게 소유자가 아니면 예외를 던진다")
        void deleteMenuOption_UnauthorizedUser_ThrowsException() {
            // Given
            CurrentUser unauthorizedUser = new CurrentUser(UUID.randomUUID(), "ROLE_OWNER");
            when(menuOptionRepository.findByIdWithMenu(optionId)).thenReturn(Optional.of(menuOption));

            // When & Then
            assertThatThrownBy(() -> menuCommandService.deleteMenuOption(optionId, unauthorizedUser))
                    .isInstanceOf(MenuException.class)
                    .hasFieldOrPropertyWithValue("code", MenuErrorCode.UNAUTHORIZED_ACCESS);
        }
    }
}