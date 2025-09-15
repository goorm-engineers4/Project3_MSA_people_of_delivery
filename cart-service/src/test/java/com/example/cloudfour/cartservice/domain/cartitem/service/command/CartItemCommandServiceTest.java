package com.example.cloudfour.cartservice.domain.cartitem.service.command;

import com.example.cloudfour.cartservice.client.StoreClient;
import com.example.cloudfour.cartservice.domain.cart.entity.Cart;
import com.example.cloudfour.cartservice.domain.cart.exception.CartException;
import com.example.cloudfour.cartservice.domain.cart.exception.CartErrorCode;
import com.example.cloudfour.cartservice.domain.cart.repository.CartRepository;
import com.example.cloudfour.cartservice.domain.cartitem.dto.CartItemRequestDTO;
import com.example.cloudfour.cartservice.domain.cartitem.dto.CartItemResponseDTO;
import com.example.cloudfour.cartservice.domain.cartitem.entity.CartItem;
import com.example.cloudfour.cartservice.domain.cartitem.exception.CartItemException;
import com.example.cloudfour.cartservice.domain.cartitem.exception.CartItemErrorCode;
import com.example.cloudfour.cartservice.domain.cartitem.repository.CartItemRepository;
import com.example.cloudfour.cartservice.domain.cartitem.converter.CartItemConverter;

import com.example.cloudfour.cartservice.commondto.MenuResponseDTO;
import com.example.cloudfour.cartservice.commondto.MenuOptionResponseDTO;

import com.example.cloudfour.modulecommon.dto.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartItemCommandService 단위테스트")
class CartItemCommandServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private StoreClient storeClient;

    @InjectMocks
    private CartItemCommandService cartItemCommandService;

    private UUID userId;
    private UUID cartId;
    private UUID menuId;
    private UUID cartItemId;
    private CurrentUser currentUser;
    private Cart cart;
    private CartItem cartItem;
    private MenuResponseDTO menu;
    private CartItemRequestDTO.CartItemAddRequestDTO addRequestDTO;
    private CartItemRequestDTO.CartItemUpdateRequestDTO updateRequestDTO;
    private CartItemResponseDTO.CartItemAddResponseDTO addResponseDTO;
    private CartItemResponseDTO.CartItemUpdateResponseDTO updateResponseDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        cartId = UUID.randomUUID();
        menuId = UUID.randomUUID();
        cartItemId = UUID.randomUUID();
        currentUser = new CurrentUser(userId, "testUser");

        // Mock Cart
        cart = mock(Cart.class);
        lenient().when(cart.getId()).thenReturn(cartId);
        lenient().when(cart.getUser()).thenReturn(userId);

        // Mock CartItem
        cartItem = mock(CartItem.class);
        lenient().when(cartItem.getId()).thenReturn(cartItemId);
        lenient().when(cartItem.getQuantity()).thenReturn(1);
        lenient().when(cartItem.getPrice()).thenReturn(10000);
        lenient().when(cartItem.getMenu()).thenReturn(menuId);

        // Mock Menu
        menu = mock(MenuResponseDTO.class);
        lenient().when(menu.getMenuId()).thenReturn(menuId);
        lenient().when(menu.getPrice()).thenReturn(10000);

        // Mock DTOs
        addRequestDTO = CartItemRequestDTO.CartItemAddRequestDTO.builder()
                .menuId(menuId)
                .menuOptionIds(new ArrayList<>())
                .build();

        updateRequestDTO = CartItemRequestDTO.CartItemUpdateRequestDTO.builder()
                .menuOptionIds(new ArrayList<>())
                .quantity(2)
                .build();

        addResponseDTO = mock(CartItemResponseDTO.CartItemAddResponseDTO.class);
        updateResponseDTO = mock(CartItemResponseDTO.CartItemUpdateResponseDTO.class);
    }

    @Nested
    @DisplayName("AddCartItem 메서드는")
    class AddCartItemTests {

        @Test
        @DisplayName("유효한 요청이 주어지면 장바구니 아이템을 추가한다")
        void addCartItem_ValidRequest_ReturnsCartItemAddResponse() {
            // Given
            when(cartRepository.findByIdAndUser(cartId, userId)).thenReturn(Optional.of(cart));
            when(storeClient.menuById(menuId)).thenReturn(menu);
            when(cartItemRepository.findByCartIdAndMenuId(cartId, menuId)).thenReturn(new ArrayList<>());
            
            // Answer를 사용하여 save될 때 ID를 설정
            when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> {
                CartItem item = invocation.getArgument(0);
                // 리플렉션을 사용하여 private ID 필드 설정
                java.lang.reflect.Field idField = CartItem.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(item, cartItemId);
                return item;
            });
            
            when(cartItemRepository.findByIdWithOptions(cartItemId)).thenReturn(Optional.of(cartItem));

            try (MockedStatic<CartItemConverter> mockedStatic = mockStatic(CartItemConverter.class)) {
                mockedStatic.when(() -> CartItemConverter.toCartItemAddResponseDTO(any(CartItem.class)))
                        .thenReturn(addResponseDTO);

                // When
                CartItemResponseDTO.CartItemAddResponseDTO result = cartItemCommandService.AddCartItem(addRequestDTO, cartId, currentUser);

                // Then
                assertThat(result).isEqualTo(addResponseDTO);
                verify(cartRepository).findByIdAndUser(cartId, userId);
                verify(storeClient).menuById(menuId);
                verify(cartItemRepository).findByCartIdAndMenuId(cartId, menuId);
                verify(cartItemRepository).save(any(CartItem.class));
                verify(cartItemRepository).findByIdWithOptions(cartItemId);
            }
        }

        @Test
        @DisplayName("사용자가 null이면 예외를 던진다")
        void addCartItem_NullUser_ThrowsException() {
            // Given
            CurrentUser nullUser = null;

            // When & Then
            assertThatThrownBy(() -> cartItemCommandService.AddCartItem(addRequestDTO, cartId, nullUser))
                    .isInstanceOf(CartItemException.class)
                    .hasFieldOrPropertyWithValue("code", CartItemErrorCode.UNAUTHORIZED_ACCESS);

            verifyNoInteractions(cartRepository, storeClient, cartItemRepository);
        }

        @Test
        @DisplayName("존재하지 않는 장바구니면 예외를 던진다")
        void addCartItem_CartNotFound_ThrowsException() {
            // Given
            when(cartRepository.findByIdAndUser(cartId, userId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> cartItemCommandService.AddCartItem(addRequestDTO, cartId, currentUser))
                    .isInstanceOf(CartException.class)
                    .hasFieldOrPropertyWithValue("code", CartErrorCode.NOT_FOUND);

            verify(cartRepository).findByIdAndUser(cartId, userId);
            verifyNoInteractions(storeClient, cartItemRepository);
        }

        @Test
        @DisplayName("존재하지 않는 메뉴면 예외를 던진다")
        void addCartItem_MenuNotFound_ThrowsException() {
            // Given
            when(cartRepository.findByIdAndUser(cartId, userId)).thenReturn(Optional.of(cart));
            when(storeClient.menuById(menuId)).thenThrow(new RuntimeException("Menu not found"));

            // When & Then
            assertThatThrownBy(() -> cartItemCommandService.AddCartItem(addRequestDTO, cartId, currentUser))
                    .isInstanceOf(RuntimeException.class);

            verify(cartRepository).findByIdAndUser(cartId, userId);
            verify(storeClient).menuById(menuId);
        }
    }

    @Nested
    @DisplayName("updateCartItem 메서드는")
    class UpdateCartItemTests {

        @Test
        @DisplayName("유효한 요청이 주어지면 장바구니 아이템을 수정한다")
        void updateCartItem_ValidRequest_ReturnsCartItemUpdateResponse() {
            // Given
            when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.of(cartItem));
            when(cartItemRepository.existsByCartItemAndUser(cartItemId, userId)).thenReturn(true);
            when(storeClient.menuById(menuId)).thenReturn(menu);
            when(cartItem.getOptions()).thenReturn(new ArrayList<>());
            when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);
            when(cartItemRepository.findByIdWithOptions(cartItemId)).thenReturn(Optional.of(cartItem));

            try (MockedStatic<CartItemConverter> mockedStatic = mockStatic(CartItemConverter.class)) {
                mockedStatic.when(() -> CartItemConverter.toCartItemUpdateResponseDTO(any(CartItem.class)))
                        .thenReturn(updateResponseDTO);

                // When
                CartItemResponseDTO.CartItemUpdateResponseDTO result = cartItemCommandService.updateCartItem(updateRequestDTO, cartItemId, currentUser);

                // Then
                assertThat(result).isEqualTo(updateResponseDTO);
                verify(cartItemRepository).findById(cartItemId);
                verify(cartItemRepository).existsByCartItemAndUser(cartItemId, userId);
                verify(storeClient).menuById(menuId);
                verify(cartItemRepository).save(any(CartItem.class));
                verify(cartItemRepository).findByIdWithOptions(cartItemId);
            }
        }

        @Test
        @DisplayName("존재하지 않는 장바구니 아이템이면 예외를 던진다")
        void updateCartItem_CartItemNotFound_ThrowsException() {
            // Given
            when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> cartItemCommandService.updateCartItem(updateRequestDTO, cartItemId, currentUser))
                    .isInstanceOf(CartItemException.class)
                    .hasFieldOrPropertyWithValue("code", CartItemErrorCode.NOT_FOUND);

            verify(cartItemRepository).findById(cartItemId);
            verifyNoInteractions(storeClient);
        }

        @Test
        @DisplayName("권한이 없으면 예외를 던진다")
        void updateCartItem_UnauthorizedAccess_ThrowsException() {
            // Given
            when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.of(cartItem));
            when(cartItemRepository.existsByCartItemAndUser(cartItemId, userId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> cartItemCommandService.updateCartItem(updateRequestDTO, cartItemId, currentUser))
                    .isInstanceOf(CartItemException.class)
                    .hasFieldOrPropertyWithValue("code", CartItemErrorCode.UNAUTHORIZED_ACCESS);

            verify(cartItemRepository).findById(cartItemId);
            verify(cartItemRepository).existsByCartItemAndUser(cartItemId, userId);
            verifyNoInteractions(storeClient);
        }

        @Test
        @DisplayName("수량이 0 이하면 예외를 던진다")
        void updateCartItem_InvalidQuantity_ThrowsException() {
            // Given
            CartItemRequestDTO.CartItemUpdateRequestDTO invalidRequest = CartItemRequestDTO.CartItemUpdateRequestDTO.builder()
                    .menuOptionIds(new ArrayList<>())
                    .quantity(0)
                    .build();

            when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.of(cartItem));
            when(cartItemRepository.existsByCartItemAndUser(cartItemId, userId)).thenReturn(true);
            when(storeClient.menuById(menuId)).thenReturn(menu);

            // When & Then
            assertThatThrownBy(() -> cartItemCommandService.updateCartItem(invalidRequest, cartItemId, currentUser))
                    .isInstanceOf(CartItemException.class)
                    .hasFieldOrPropertyWithValue("code", CartItemErrorCode.INVALID_QUANTITY);
        }
    }

    @Nested
    @DisplayName("deleteCartItem 메서드는")
    class DeleteCartItemTests {

        @Test
        @DisplayName("유효한 요청이 주어지면 장바구니 아이템을 삭제한다")
        void deleteCartItem_ValidRequest_DeletesCartItem() {
            // Given
            when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.of(cartItem));
            when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));
            when(cartItemRepository.existsByCartItemAndUser(cartItemId, userId)).thenReturn(true);
            when(cartItem.getCart()).thenReturn(cart);

            // When
            cartItemCommandService.deleteCartItem(cartItemId, currentUser);

            // Then
            verify(cartItemRepository).findById(cartItemId);
            verify(cartRepository).findById(cartId);
            verify(cartItemRepository).existsByCartItemAndUser(cartItemId, userId);
            verify(cartItemRepository).delete(cartItem);
        }

        @Test
        @DisplayName("존재하지 않는 장바구니 아이템이면 예외를 던진다")
        void deleteCartItem_CartItemNotFound_ThrowsException() {
            // Given
            when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> cartItemCommandService.deleteCartItem(cartItemId, currentUser))
                    .isInstanceOf(CartItemException.class)
                    .hasFieldOrPropertyWithValue("code", CartItemErrorCode.NOT_FOUND);

            verify(cartItemRepository).findById(cartItemId);
            verify(cartItemRepository, never()).delete(any());
        }

        @Test
        @DisplayName("권한이 없으면 예외를 던진다")
        void deleteCartItem_UnauthorizedAccess_ThrowsException() {
            // Given
            when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.of(cartItem));
            when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));
            when(cartItemRepository.existsByCartItemAndUser(cartItemId, userId)).thenReturn(false);
            when(cartItem.getCart()).thenReturn(cart);

            // When & Then
            assertThatThrownBy(() -> cartItemCommandService.deleteCartItem(cartItemId, currentUser))
                    .isInstanceOf(CartItemException.class)
                    .hasFieldOrPropertyWithValue("code", CartItemErrorCode.UNAUTHORIZED_ACCESS);

            verify(cartItemRepository).findById(cartItemId);
            verify(cartRepository).findById(cartId);
            verify(cartItemRepository).existsByCartItemAndUser(cartItemId, userId);
            verify(cartItemRepository, never()).delete(any());
        }
    }
}