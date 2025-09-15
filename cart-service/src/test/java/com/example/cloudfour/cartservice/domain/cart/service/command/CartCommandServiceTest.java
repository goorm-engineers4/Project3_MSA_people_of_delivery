package com.example.cloudfour.cartservice.domain.cart.service.command;

import com.example.cloudfour.cartservice.client.StoreClient;
import com.example.cloudfour.cartservice.domain.cart.controller.CartCommonResponseDTO;
import com.example.cloudfour.cartservice.domain.cart.converter.CartConverter;
import com.example.cloudfour.cartservice.domain.cart.dto.CartRequestDTO;
import com.example.cloudfour.cartservice.domain.cart.dto.CartResponseDTO;
import com.example.cloudfour.cartservice.domain.cart.entity.Cart;
import com.example.cloudfour.cartservice.domain.cart.exception.CartErrorCode;
import com.example.cloudfour.cartservice.domain.cart.exception.CartException;
import com.example.cloudfour.cartservice.domain.cart.repository.CartRepository;
import com.example.cloudfour.cartservice.domain.cartitem.controller.CartItemCommonResponseDTO;
import com.example.cloudfour.cartservice.domain.cartitem.converter.CartItemConverter;
import com.example.cloudfour.cartservice.domain.cartitem.dto.CartItemRequestDTO;
import com.example.cloudfour.cartservice.domain.cartitem.dto.CartItemResponseDTO;
import com.example.cloudfour.cartservice.domain.cartitem.service.command.CartItemCommandService;
import com.example.cloudfour.cartservice.commondto.MenuResponseDTO;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartCommandService 단위테스트")
class CartCommandServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemCommandService cartItemCommandService;

    @Mock
    private StoreClient storeClient;

    @InjectMocks
    private CartCommandService cartCommandService;

    private UUID userId;
    private UUID storeId;
    private UUID menuId;
    private UUID cartId;
    private UUID cartItemId;
    private CurrentUser currentUser;
    private Cart cart;
    private MenuResponseDTO menu;
    private CartRequestDTO.CartCreateRequestDTO createRequestDTO;
    private CartItemRequestDTO.CartItemAddRequestDTO cartItemAddRequestDTO;
    private CartItemResponseDTO.CartItemAddResponseDTO cartItemAddResponseDTO;
    private CartItemResponseDTO cartItemCommonResponseDTO;
    private CartResponseDTO.CartCreateResponseDTO cartCreateResponseDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        menuId = UUID.randomUUID();
        cartId = UUID.randomUUID();
        cartItemId = UUID.randomUUID();
        currentUser = new CurrentUser(userId, "testUser");

        // Mock Cart
        cart = mock(Cart.class);
        lenient().when(cart.getId()).thenReturn(cartId);
        lenient().when(cart.getUser()).thenReturn(userId);
        lenient().when(cart.getStore()).thenReturn(storeId);

        // Mock Menu
        menu = mock(MenuResponseDTO.class);
        lenient().when(menu.getPrice()).thenReturn(10000);

        // Mock DTOs
        createRequestDTO = CartRequestDTO.CartCreateRequestDTO.builder()
                .storeId(storeId)
                .menuId(menuId)
                .build();

        // CartItemAddRequestDTO 초기화 추가
        cartItemAddRequestDTO = mock(CartItemRequestDTO.CartItemAddRequestDTO.class);

        // CartItemCommonResponseDTO를 올바르게 모킹
        CartItemCommonResponseDTO cartItemCommonResponseDTO = mock(CartItemCommonResponseDTO.class);
        lenient().when(cartItemCommonResponseDTO.getCartItemId()).thenReturn(cartItemId);

        cartItemAddResponseDTO = mock(CartItemResponseDTO.CartItemAddResponseDTO.class);
        lenient().when(cartItemAddResponseDTO.getCartItemCommonResponseDTO()).thenReturn(cartItemCommonResponseDTO);

        // CartCommonResponseDTO 모킹 추가
        CartCommonResponseDTO cartCommonResponseDTO = mock(CartCommonResponseDTO.class);
        lenient().when(cartCommonResponseDTO.getCartId()).thenReturn(cartId);

        cartCreateResponseDTO = mock(CartResponseDTO.CartCreateResponseDTO.class);
        lenient().when(cartCreateResponseDTO.getCartCommonResponseDTO()).thenReturn(cartCommonResponseDTO);
        lenient().when(cartCreateResponseDTO.getCartItemId()).thenReturn(cartItemId);
    }

    @Nested
    @DisplayName("createCart 메서드는")
    class CreateCartTests {

        @Test
        @DisplayName("유효한 요청이 주어지면 장바구니를 생성한다")
        void createCart_ValidRequest_ReturnsCartCreateResponse() {
            // Given
            when(storeClient.existStore(storeId)).thenReturn(true);
            when(cartRepository.existsByUserAndStore(userId, storeId)).thenReturn(false);
            when(cartRepository.save(any(Cart.class))).thenReturn(cart);
            when(storeClient.menuById(menuId)).thenReturn(menu);
            when(cartItemCommandService.CreateCartItem(
                any(CartItemRequestDTO.CartItemAddRequestDTO.class), 
                eq(cartId), 
                eq(currentUser)))
                .thenReturn(cartItemAddResponseDTO);

            try (MockedStatic<CartItemConverter> cartItemConverterMock = mockStatic(CartItemConverter.class);
                 MockedStatic<CartConverter> cartConverterMock = mockStatic(CartConverter.class)) {

                // MockedStatic에서는 any() 매처 사용
                cartItemConverterMock.when(() -> CartItemConverter.toCartItemAddRequestDTO(any(CartRequestDTO.CartCreateRequestDTO.class), any(Integer.class)))
                        .thenReturn(cartItemAddRequestDTO);
                cartConverterMock.when(() -> CartConverter.toCartCreateResponseDTO(any(Cart.class), any(UUID.class)))
                        .thenReturn(cartCreateResponseDTO);

                // When
                CartResponseDTO.CartCreateResponseDTO result = cartCommandService.createCart(createRequestDTO, currentUser);

                // Then
                assertThat(result).isEqualTo(cartCreateResponseDTO);
                verify(storeClient).existStore(storeId);
                verify(cartRepository).existsByUserAndStore(userId, storeId);
                verify(cartRepository).save(any(Cart.class));
                verify(storeClient).menuById(menuId);
                verify(cartItemCommandService).CreateCartItem(
                    any(CartItemRequestDTO.CartItemAddRequestDTO.class), 
                    eq(cartId), 
                    eq(currentUser));
            }
        }

        @Test
        @DisplayName("사용자가 null이면 예외를 던진다")
        void createCart_NullUser_ThrowsException() {
            // Given
            CurrentUser nullUser = null;

            // When & Then
            assertThatThrownBy(() -> cartCommandService.createCart(createRequestDTO, nullUser))
                    .isInstanceOf(CartException.class)
                    .hasFieldOrPropertyWithValue("code", CartErrorCode.UNAUTHORIZED_ACCESS);

            verifyNoInteractions(storeClient, cartRepository, cartItemCommandService);
        }

        @Test
        @DisplayName("사용자 ID가 null이면 예외를 던진다")
        void createCart_NullUserId_ThrowsException() {
            // Given
            CurrentUser invalidUser = new CurrentUser(null, "testUser");

            // When & Then
            assertThatThrownBy(() -> cartCommandService.createCart(createRequestDTO, invalidUser))
                    .isInstanceOf(CartException.class)
                    .hasFieldOrPropertyWithValue("code", CartErrorCode.UNAUTHORIZED_ACCESS);

            verifyNoInteractions(storeClient, cartRepository, cartItemCommandService);
        }

        @Test
        @DisplayName("스토어 ID가 null이면 예외를 던진다")
        void createCart_NullStoreId_ThrowsException() {
            // Given
            CartRequestDTO.CartCreateRequestDTO requestWithNullStoreId = CartRequestDTO.CartCreateRequestDTO.builder()
                    .storeId(null)
                    .menuId(menuId)
                    .build();

            // When & Then
            assertThatThrownBy(() -> cartCommandService.createCart(requestWithNullStoreId, currentUser))
                    .isInstanceOf(CartException.class)
                    .hasFieldOrPropertyWithValue("code", CartErrorCode.STORE_NOT_FOUND);

            verifyNoInteractions(storeClient, cartRepository, cartItemCommandService);
        }

        @Test
        @DisplayName("존재하지 않는 스토어면 예외를 던진다")
        void createCart_StoreNotFound_ThrowsException() {
            // Given
            when(storeClient.existStore(storeId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> cartCommandService.createCart(createRequestDTO, currentUser))
                    .isInstanceOf(CartException.class)
                    .hasFieldOrPropertyWithValue("code", CartErrorCode.STORE_NOT_FOUND);

            verify(storeClient).existStore(storeId);
            verifyNoInteractions(cartRepository, cartItemCommandService);
        }

        @Test
        @DisplayName("이미 해당 스토어에 장바구니가 있으면 예외를 던진다")
        void createCart_DuplicateCart_ThrowsException() {
            // Given
            when(storeClient.existStore(storeId)).thenReturn(true);
            when(cartRepository.existsByUserAndStore(userId, storeId)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> cartCommandService.createCart(createRequestDTO, currentUser))
                    .isInstanceOf(CartException.class)
                    .hasFieldOrPropertyWithValue("code", CartErrorCode.ALREADY_ADD);

            verify(storeClient).existStore(storeId);
            verify(cartRepository).existsByUserAndStore(userId, storeId);
            verify(cartRepository, never()).save(any());
            verifyNoInteractions(cartItemCommandService);
        }
    }

    @Nested
    @DisplayName("deleteCart 메서드는")
    class DeleteCartTests {

        @Test
        @DisplayName("유효한 요청이 주어지면 장바구니를 삭제한다")
        void deleteCart_ValidRequest_DeletesCart() {
            // Given
            when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));

            // When
            cartCommandService.deleteCart(cartId, currentUser);

            // Then
            verify(cartRepository).findById(cartId);
            verify(cartRepository).delete(cart);
        }

        @Test
        @DisplayName("사용자가 null이면 예외를 던진다")
        void deleteCart_NullUser_ThrowsException() {
            // Given
            CurrentUser nullUser = null;

            // When & Then
            assertThatThrownBy(() -> cartCommandService.deleteCart(cartId, nullUser))
                    .isInstanceOf(CartException.class)
                    .hasFieldOrPropertyWithValue("code", CartErrorCode.UNAUTHORIZED_ACCESS);

            verifyNoInteractions(cartRepository);
        }

        @Test
        @DisplayName("사용자 ID가 null이면 예외를 던진다")
        void deleteCart_NullUserId_ThrowsException() {
            // Given
            CurrentUser invalidUser = new CurrentUser(null, "testUser");

            // When & Then
            assertThatThrownBy(() -> cartCommandService.deleteCart(cartId, invalidUser))
                    .isInstanceOf(CartException.class)
                    .hasFieldOrPropertyWithValue("code", CartErrorCode.UNAUTHORIZED_ACCESS);

            verifyNoInteractions(cartRepository);
        }

        @Test
        @DisplayName("존재하지 않는 장바구니면 예외를 던진다")
        void deleteCart_CartNotFound_ThrowsException() {
            // Given
            when(cartRepository.findById(cartId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> cartCommandService.deleteCart(cartId, currentUser))
                    .isInstanceOf(CartException.class)
                    .hasFieldOrPropertyWithValue("code", CartErrorCode.NOT_FOUND);

            verify(cartRepository).findById(cartId);
            verify(cartRepository, never()).delete(any());
        }

        @Test
        @DisplayName("장바구니 소유자가 아니면 예외를 던진다")
        void deleteCart_UnauthorizedAccess_ThrowsException() {
            // Given
            UUID otherUserId = UUID.randomUUID();
            Cart otherUserCart = mock(Cart.class);
            when(otherUserCart.getId()).thenReturn(cartId);
            when(otherUserCart.getUser()).thenReturn(otherUserId);

            when(cartRepository.findById(cartId)).thenReturn(Optional.of(otherUserCart));

            // When & Then
            assertThatThrownBy(() -> cartCommandService.deleteCart(cartId, currentUser))
                    .isInstanceOf(CartException.class)
                    .hasFieldOrPropertyWithValue("code", CartErrorCode.UNAUTHORIZED_ACCESS);

            verify(cartRepository).findById(cartId);
            verify(cartRepository, never()).delete(any());
        }
    }
}