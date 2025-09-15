package com.example.cloudfour.cartservice.domain.cartitem.service.query;

import com.example.cloudfour.cartservice.domain.cartitem.dto.CartItemResponseDTO;
import com.example.cloudfour.cartservice.domain.cartitem.entity.CartItem;
import com.example.cloudfour.cartservice.domain.cartitem.exception.CartItemException;
import com.example.cloudfour.cartservice.domain.cartitem.exception.CartItemErrorCode;
import com.example.cloudfour.cartservice.domain.cartitem.repository.CartItemRepository;
import com.example.cloudfour.cartservice.domain.cartitem.converter.CartItemConverter;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartItemQueryService 단위테스트")
class CartItemQueryServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private CartItemQueryService cartItemQueryService;

    private UUID userId;
    private UUID cartItemId;
    private CurrentUser currentUser;
    private CartItem cartItem;
    private CartItemResponseDTO.CartItemListResponseDTO cartItemListResponseDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        cartItemId = UUID.randomUUID();
        currentUser = new CurrentUser(userId, "testUser");

        // Mock CartItem
        cartItem = mock(CartItem.class);
        lenient().when(cartItem.getId()).thenReturn(cartItemId);
        lenient().when(cartItem.getQuantity()).thenReturn(2);
        lenient().when(cartItem.getPrice()).thenReturn(20000);

        // Mock Response DTO
        cartItemListResponseDTO = mock(CartItemResponseDTO.CartItemListResponseDTO.class);
    }

    @Nested
    @DisplayName("getCartItemById 메서드는")
    class GetCartItemByIdTests {

        @Test
        @DisplayName("유효한 요청이 주어지면 장바구니 아이템을 반환한다")
        void getCartItemById_ValidRequest_ReturnsCartItem() {
            // Given
            when(cartItemRepository.existsByCartItemAndUser(cartItemId, userId)).thenReturn(true);
            when(cartItemRepository.findByIdWithOptions(cartItemId)).thenReturn(Optional.of(cartItem));

            try (MockedStatic<CartItemConverter> mockedStatic = mockStatic(CartItemConverter.class)) {
                mockedStatic.when(() -> CartItemConverter.toCartItemListResponseDTO(cartItem))
                        .thenReturn(cartItemListResponseDTO);

                // When
                CartItemResponseDTO.CartItemListResponseDTO result = cartItemQueryService.getCartItemById(cartItemId, currentUser);

                // Then
                assertThat(result).isEqualTo(cartItemListResponseDTO);
                verify(cartItemRepository).existsByCartItemAndUser(cartItemId, userId);
                verify(cartItemRepository).findByIdWithOptions(cartItemId);
                mockedStatic.verify(() -> CartItemConverter.toCartItemListResponseDTO(cartItem));
            }
        }

        @Test
        @DisplayName("사용자가 null이면 예외를 던진다")
        void getCartItemById_NullUser_ThrowsException() {
            // Given
            CurrentUser nullUser = null;

            // When & Then
            assertThatThrownBy(() -> cartItemQueryService.getCartItemById(cartItemId, nullUser))
                    .isInstanceOf(CartItemException.class)
                    .hasFieldOrPropertyWithValue("code", CartItemErrorCode.UNAUTHORIZED_ACCESS);

            verifyNoInteractions(cartItemRepository);
        }

        @Test
        @DisplayName("사용자 ID가 null이면 예외를 던진다")
        void getCartItemById_NullUserId_ThrowsException() {
            // Given
            CurrentUser invalidUser = new CurrentUser(null, "testUser");

            // When & Then
            assertThatThrownBy(() -> cartItemQueryService.getCartItemById(cartItemId, invalidUser))
                    .isInstanceOf(CartItemException.class)
                    .hasFieldOrPropertyWithValue("code", CartItemErrorCode.UNAUTHORIZED_ACCESS);

            verifyNoInteractions(cartItemRepository);
        }

        @Test
        @DisplayName("장바구니 아이템 ID가 null이면 예외를 던진다")
        void getCartItemById_NullCartItemId_ThrowsException() {
            // Given
            UUID nullCartItemId = null;

            // When & Then
            assertThatThrownBy(() -> cartItemQueryService.getCartItemById(nullCartItemId, currentUser))
                    .isInstanceOf(CartItemException.class)
                    .hasFieldOrPropertyWithValue("code", CartItemErrorCode.NOT_FOUND);

            verifyNoInteractions(cartItemRepository);
        }

        @Test
        @DisplayName("장바구니 아이템 소유권이 없으면 예외를 던진다")
        void getCartItemById_UnauthorizedAccess_ThrowsException() {
            // Given
            when(cartItemRepository.existsByCartItemAndUser(cartItemId, userId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> cartItemQueryService.getCartItemById(cartItemId, currentUser))
                    .isInstanceOf(CartItemException.class)
                    .hasFieldOrPropertyWithValue("code", CartItemErrorCode.UNAUTHORIZED_ACCESS);

            verify(cartItemRepository).existsByCartItemAndUser(cartItemId, userId);
            verify(cartItemRepository, never()).findByIdWithOptions(any());
        }

        @Test
        @DisplayName("존재하지 않는 장바구니 아이템이면 예외를 던진다")
        void getCartItemById_CartItemNotFound_ThrowsException() {
            // Given
            when(cartItemRepository.existsByCartItemAndUser(cartItemId, userId)).thenReturn(true);
            when(cartItemRepository.findByIdWithOptions(cartItemId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> cartItemQueryService.getCartItemById(cartItemId, currentUser))
                    .isInstanceOf(CartItemException.class)
                    .hasFieldOrPropertyWithValue("code", CartItemErrorCode.NOT_FOUND);

            verify(cartItemRepository).existsByCartItemAndUser(cartItemId, userId);
            verify(cartItemRepository).findByIdWithOptions(cartItemId);
        }

        @Test
        @DisplayName("CartItemConverter가 null을 반환해도 정상적으로 처리한다")
        void getCartItemById_ConverterReturnsNull_ReturnsNull() {
            // Given
            when(cartItemRepository.existsByCartItemAndUser(cartItemId, userId)).thenReturn(true);
            when(cartItemRepository.findByIdWithOptions(cartItemId)).thenReturn(Optional.of(cartItem));

            try (MockedStatic<CartItemConverter> mockedStatic = mockStatic(CartItemConverter.class)) {
                mockedStatic.when(() -> CartItemConverter.toCartItemListResponseDTO(cartItem))
                        .thenReturn(null);

                // When
                CartItemResponseDTO.CartItemListResponseDTO result = cartItemQueryService.getCartItemById(cartItemId, currentUser);

                // Then
                assertThat(result).isNull();
                verify(cartItemRepository).existsByCartItemAndUser(cartItemId, userId);
                verify(cartItemRepository).findByIdWithOptions(cartItemId);
                mockedStatic.verify(() -> CartItemConverter.toCartItemListResponseDTO(cartItem));
            }
        }
    }
}