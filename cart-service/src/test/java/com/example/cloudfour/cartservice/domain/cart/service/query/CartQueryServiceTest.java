package com.example.cloudfour.cartservice.domain.cart.service.query;

import com.example.cloudfour.cartservice.domain.cart.controller.CartCommonResponseDTO;
import com.example.cloudfour.cartservice.domain.cart.converter.CartConverter;
import com.example.cloudfour.cartservice.domain.cart.dto.CartResponseDTO;
import com.example.cloudfour.cartservice.domain.cart.entity.Cart;
import com.example.cloudfour.cartservice.domain.cart.exception.CartErrorCode;
import com.example.cloudfour.cartservice.domain.cart.exception.CartException;
import com.example.cloudfour.cartservice.domain.cart.repository.CartRepository;
import com.example.cloudfour.cartservice.domain.cartitem.entity.CartItem;
import com.example.cloudfour.cartservice.domain.cartitem.repository.CartItemRepository;
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
@DisplayName("CartQueryService 단위테스트")
class CartQueryServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private CartQueryService cartQueryService;

    private UUID userId;
    private UUID cartId;
    private CurrentUser currentUser;
    private Cart cart;
    private List<CartItem> cartItems;
    private CartItem cartItem1;
    private CartItem cartItem2;
    private CartResponseDTO.CartDetailResponseDTO cartDetailResponseDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        cartId = UUID.randomUUID();
        currentUser = new CurrentUser(userId, "testUser");

        // Mock Cart
        cart = mock(Cart.class);
        lenient().when(cart.getId()).thenReturn(cartId);
        lenient().when(cart.getUser()).thenReturn(userId);
        lenient().when(cart.getCartItems()).thenReturn(new ArrayList<>());

        // Mock CartItems
        cartItem1 = mock(CartItem.class);
        lenient().when(cartItem1.getId()).thenReturn(UUID.randomUUID());

        cartItem2 = mock(CartItem.class);
        lenient().when(cartItem2.getId()).thenReturn(UUID.randomUUID());

        cartItems = new ArrayList<>();
        cartItems.add(cartItem1);
        cartItems.add(cartItem2);

        // Mock CartCommonResponseDTO 추가
        CartCommonResponseDTO cartCommonResponseDTO = mock(CartCommonResponseDTO.class);
        lenient().when(cartCommonResponseDTO.getCartId()).thenReturn(cartId);

        // Mock Response DTO
        cartDetailResponseDTO = mock(CartResponseDTO.CartDetailResponseDTO.class);
        lenient().when(cartDetailResponseDTO.getCartCommonResponseDTO()).thenReturn(cartCommonResponseDTO);
    }

    @Nested
    @DisplayName("getCartListById 메서드는")
    class GetCartListByIdTests {

        @Test
        @DisplayName("유효한 요청이 주어지면 장바구니 상세 정보를 반환한다")
        void getCartListById_ValidRequest_ReturnsCartDetail() {
            // Given
            when(cartRepository.findByIdAndUserWithCartItems(cartId, userId)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findAllByCartIdWithOptions(cartId)).thenReturn(cartItems);

            try (MockedStatic<CartConverter> mockedStatic = mockStatic(CartConverter.class)) {
                mockedStatic.when(() -> CartConverter.toCartDetailResponseDTO(cart))
                        .thenReturn(cartDetailResponseDTO);

                // When
                CartResponseDTO.CartDetailResponseDTO result = cartQueryService.getCartListById(cartId, currentUser);

                // Then
                assertThat(result).isEqualTo(cartDetailResponseDTO);
                verify(cartRepository).findByIdAndUserWithCartItems(cartId, userId);
                verify(cartItemRepository).findAllByCartIdWithOptions(cartId);
                verify(cart, times(2)).getCartItems(); // clear() 1번 + addAll() 1번 = 2번
                mockedStatic.verify(() -> CartConverter.toCartDetailResponseDTO(cart));
            }
        }

        @Test
        @DisplayName("사용자가 null이면 예외를 던진다")
        void getCartListById_NullUser_ThrowsException() {
            // Given
            CurrentUser nullUser = null;

            // When & Then
            assertThatThrownBy(() -> cartQueryService.getCartListById(cartId, nullUser))
                    .isInstanceOf(CartException.class)
                    .hasFieldOrPropertyWithValue("code", CartErrorCode.UNAUTHORIZED_ACCESS);

            verifyNoInteractions(cartRepository, cartItemRepository);
        }

        @Test
        @DisplayName("사용자 ID가 null이면 예외를 던진다")
        void getCartListById_NullUserId_ThrowsException() {
            // Given
            CurrentUser invalidUser = new CurrentUser(null, "testUser");

            // When & Then
            assertThatThrownBy(() -> cartQueryService.getCartListById(cartId, invalidUser))
                    .isInstanceOf(CartException.class)
                    .hasFieldOrPropertyWithValue("code", CartErrorCode.UNAUTHORIZED_ACCESS);

            verifyNoInteractions(cartRepository, cartItemRepository);
        }

        @Test
        @DisplayName("장바구니 ID가 null이면 예외를 던진다")
        void getCartListById_NullCartId_ThrowsException() {
            // Given
            UUID nullCartId = null;

            // When & Then
            assertThatThrownBy(() -> cartQueryService.getCartListById(nullCartId, currentUser))
                    .isInstanceOf(CartException.class)
                    .hasFieldOrPropertyWithValue("code", CartErrorCode.NOT_FOUND);

            verifyNoInteractions(cartRepository, cartItemRepository);
        }

        @Test
        @DisplayName("존재하지 않는 장바구니이거나 접근 권한이 없으면 예외를 던진다")
        void getCartListById_CartNotFoundOrUnauthorized_ThrowsException() {
            // Given
            when(cartRepository.findByIdAndUserWithCartItems(cartId, userId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> cartQueryService.getCartListById(cartId, currentUser))
                    .isInstanceOf(CartException.class)
                    .hasFieldOrPropertyWithValue("code", CartErrorCode.NOT_FOUND);

            verify(cartRepository).findByIdAndUserWithCartItems(cartId, userId);
            verifyNoInteractions(cartItemRepository);
        }

        @Test
        @DisplayName("장바구니 아이템이 없어도 정상적으로 처리한다")
        void getCartListById_EmptyCartItems_ReturnsCartDetail() {
            // Given
            List<CartItem> emptyCartItems = new ArrayList<>();
            when(cartRepository.findByIdAndUserWithCartItems(cartId, userId)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findAllByCartIdWithOptions(cartId)).thenReturn(emptyCartItems);

            try (MockedStatic<CartConverter> mockedStatic = mockStatic(CartConverter.class)) {
                mockedStatic.when(() -> CartConverter.toCartDetailResponseDTO(cart))
                        .thenReturn(cartDetailResponseDTO);

                // When
                CartResponseDTO.CartDetailResponseDTO result = cartQueryService.getCartListById(cartId, currentUser);

                // Then
                assertThat(result).isEqualTo(cartDetailResponseDTO);
                verify(cartRepository).findByIdAndUserWithCartItems(cartId, userId);
                verify(cartItemRepository).findAllByCartIdWithOptions(cartId);
                verify(cart).getCartItems();
                mockedStatic.verify(() -> CartConverter.toCartDetailResponseDTO(cart));
            }
        }

        @Test
        @DisplayName("장바구니 아이템을 옵션과 함께 로드한다")
        void getCartListById_LoadsCartItemsWithOptions() {
            // Given
            when(cartRepository.findByIdAndUserWithCartItems(cartId, userId)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findAllByCartIdWithOptions(cartId)).thenReturn(cartItems);

            try (MockedStatic<CartConverter> mockedStatic = mockStatic(CartConverter.class)) {
                mockedStatic.when(() -> CartConverter.toCartDetailResponseDTO(cart))
                        .thenReturn(cartDetailResponseDTO);

                // When
                cartQueryService.getCartListById(cartId, currentUser);

                // Then
                // findAllByCartIdWithOptions 메서드가 호출되었는지 확인
                verify(cartItemRepository).findAllByCartIdWithOptions(cartId);
                // 존재하지 않는 메서드 호출 확인 제거
            }
        }

        @Test
        @DisplayName("장바구니 아이템을 교체한다")
        void getCartListById_ReplacesCartItems() {
            // Given
            when(cartRepository.findByIdAndUserWithCartItems(cartId, userId)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findAllByCartIdWithOptions(cartId)).thenReturn(cartItems);

            try (MockedStatic<CartConverter> mockedStatic = mockStatic(CartConverter.class)) {
                mockedStatic.when(() -> CartConverter.toCartDetailResponseDTO(cart))
                        .thenReturn(cartDetailResponseDTO);

                // When
                cartQueryService.getCartListById(cartId, currentUser);

                // Then
                // cart.getCartItems().clear()와 addAll이 호출되었는지 확인
                verify(cart, times(2)).getCartItems(); // clear() 호출 시 1번, addAll() 호출 시 1번
            }
        }

        @Test
        @DisplayName("CartConverter가 null을 반환해도 정상적으로 처리한다")
        void getCartListById_ConverterReturnsNull_ReturnsNull() {
            // Given
            when(cartRepository.findByIdAndUserWithCartItems(cartId, userId)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findAllByCartIdWithOptions(cartId)).thenReturn(cartItems);

            try (MockedStatic<CartConverter> mockedStatic = mockStatic(CartConverter.class)) {
                mockedStatic.when(() -> CartConverter.toCartDetailResponseDTO(cart))
                        .thenReturn(null);

                // When
                CartResponseDTO.CartDetailResponseDTO result = cartQueryService.getCartListById(cartId, currentUser);

                // Then
                assertThat(result).isNull();
                verify(cartRepository).findByIdAndUserWithCartItems(cartId, userId);
                verify(cartItemRepository).findAllByCartIdWithOptions(cartId);
                mockedStatic.verify(() -> CartConverter.toCartDetailResponseDTO(cart));
            }
        }
    }
}