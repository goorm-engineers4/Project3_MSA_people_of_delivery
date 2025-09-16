package com.example.cloudfour.cartservice.domain.order.service.command;

import com.example.cloudfour.cartservice.client.StoreClient;
import com.example.cloudfour.cartservice.client.UserClient;
import com.example.cloudfour.cartservice.commondto.UserAddressResponseDTO;
import com.example.cloudfour.cartservice.domain.cart.entity.Cart;
import com.example.cloudfour.cartservice.domain.cart.exception.CartErrorCode;
import com.example.cloudfour.cartservice.domain.cart.exception.CartException;
import com.example.cloudfour.cartservice.domain.cart.repository.CartRepository;
import com.example.cloudfour.cartservice.domain.cartitem.entity.CartItem;
import com.example.cloudfour.cartservice.domain.cartitem.exception.CartItemErrorCode;
import com.example.cloudfour.cartservice.domain.cartitem.exception.CartItemException;
import com.example.cloudfour.cartservice.domain.order.converter.OrderConverter;
import com.example.cloudfour.cartservice.domain.order.converter.OrderItemConverter;
import com.example.cloudfour.cartservice.domain.order.dto.OrderRequestDTO;
import com.example.cloudfour.cartservice.domain.order.dto.OrderResponseDTO;
import com.example.cloudfour.cartservice.domain.order.entity.Order;
import com.example.cloudfour.cartservice.domain.order.entity.OrderItem;
import com.example.cloudfour.cartservice.domain.order.enums.OrderStatus;
import com.example.cloudfour.cartservice.domain.order.exception.OrderErrorCode;
import com.example.cloudfour.cartservice.domain.order.exception.OrderException;
import com.example.cloudfour.cartservice.domain.order.repository.OrderItemOptionRepository;
import com.example.cloudfour.cartservice.domain.order.repository.OrderItemRepository;
import com.example.cloudfour.cartservice.domain.order.repository.OrderRepository;
import com.example.cloudfour.cartservice.domain.order.service.event.OrderEventService;
import com.example.cloudfour.modulecommon.dto.Passport;
import com.example.cloudfour.modulecommon.schedule.OrderTimeoutHandler;
import com.example.cloudfour.modulecommon.schedule.ScheduledTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCommandService 단위 테스트")
class OrderCommandServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock OrderItemOptionRepository orderItemOptionRepository;
    @Mock CartRepository cartRepository;
    @Mock StoreClient storeClient;
    @Mock UserClient userClient;
    @Mock OrderEventService orderEventService;
    @Mock ScheduledTaskService scheduledTaskService;
    @Mock OrderTimeoutHandler orderTimeoutHandler;

    @InjectMocks OrderCommandService service;

    UUID userId;
    UUID cartId;
    UUID storeId;
    Passport passport;

    @BeforeEach
    void init() {
        userId = UUID.randomUUID();
        cartId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        passport = Passport.builder().userId(userId).role("ROLE_CUSTOMER").build();
    }

    private OrderRequestDTO.OrderCreateRequestDTO mkOrderCreateReq() {
        return OrderRequestDTO.OrderCreateRequestDTO.builder()
                .orderType(com.example.cloudfour.cartservice.domain.order.enums.OrderType.ONLINE)
                .orderStatus(OrderStatus.PENDING)
                .receiptType(com.example.cloudfour.cartservice.domain.order.enums.ReceiptType.TOGO)
                .request("없음")
                .build();
    }

    @Nested
    @DisplayName("createOrder")
    class CreateOrder {
        @Test
        @DisplayName("성공 시 주문 생성, 아이템 저장, 이벤트 발행, 타임아웃 스케줄")
        void success() {
            var req = mkOrderCreateReq();

            Cart cart = mock(Cart.class);
            CartItem ci = mock(CartItem.class);
            when(ci.getPrice()).thenReturn(1000);
            when(cart.getCartItems()).thenReturn(List.of(ci));
            when(cart.getStore()).thenReturn(storeId);
            when(cart.getId()).thenReturn(cartId);

            when(cartRepository.findByIdAndUserWithCartItems(cartId, userId)).thenReturn(Optional.of(cart));
            when(cartRepository.existsByUserAndCart(userId, cartId)).thenReturn(true);
            when(userClient.addressById(userId)).thenReturn(UserAddressResponseDTO.builder().address("Seoul").build());
            when(storeClient.existStore(storeId)).thenReturn(true);

            Order order = mock(Order.class);
            when(order.getId()).thenReturn(UUID.randomUUID());
            OrderResponseDTO.OrderCreateResponseDTO resp = mock(OrderResponseDTO.OrderCreateResponseDTO.class);

            try (MockedStatic<OrderConverter> oc = mockStatic(OrderConverter.class);
                 MockedStatic<OrderItemConverter> oic = mockStatic(OrderItemConverter.class)) {

                oc.when(() -> OrderConverter.toOrder(eq(req), eq(1000), anyString()))
                        .thenReturn(order);
                oc.when(() -> OrderConverter.toOrderCreateResponseDTO(order)).thenReturn(resp);

                oic.when(() -> OrderItemConverter.CartItemtoOrderItem(ci, order))
                        .thenReturn(mock(OrderItem.class));

                OrderResponseDTO.OrderCreateResponseDTO out = service.createOrder(req, cartId, passport);

                assertThat(out).isSameAs(resp);
                verify(orderRepository).save(order);
                verify(orderItemRepository).saveAll(anyList());
                verify(orderEventService).publishOrderCreated(order);
                verify(scheduledTaskService).scheduleOrderTimeout(anyString(), anyDouble(), any());
                verify(cartRepository).delete(cart);
            }
        }

        @Test
        @DisplayName("장바구니 아이템 없음 예외")
        void empty_cart_items() {
            var req = mkOrderCreateReq();
            Cart cart = mock(Cart.class);
            when(cart.getCartItems()).thenReturn(List.of());
            when(cart.getStore()).thenReturn(storeId);
            when(cartRepository.findByIdAndUserWithCartItems(cartId, userId)).thenReturn(Optional.of(cart));
            when(cartRepository.existsByUserAndCart(userId, cartId)).thenReturn(true);
            when(userClient.addressById(userId)).thenReturn(UserAddressResponseDTO.builder().address("Seoul").build());
            when(storeClient.existStore(storeId)).thenReturn(true);

            assertThatThrownBy(() -> service.createOrder(req, cartId, passport))
                    .isInstanceOf(CartItemException.class)
                    .hasFieldOrPropertyWithValue("code", CartItemErrorCode.NOT_FOUND);
        }

        @Test
        @DisplayName("스토어 미존재 예외")
        void store_not_found() {
            var req = mkOrderCreateReq();
            Cart cart = mock(Cart.class);
            lenient().when(cart.getCartItems()).thenReturn(List.of(mock(CartItem.class)));
            when(cart.getStore()).thenReturn(storeId);
            when(cartRepository.findByIdAndUserWithCartItems(cartId, userId)).thenReturn(Optional.of(cart));
            when(cartRepository.existsByUserAndCart(userId, cartId)).thenReturn(true);
            when(userClient.addressById(userId)).thenReturn(UserAddressResponseDTO.builder().address("Seoul").build());
            when(storeClient.existStore(storeId)).thenReturn(false);

            assertThatThrownBy(() -> service.createOrder(req, cartId, passport))
                    .isInstanceOf(CartException.class)
                    .hasFieldOrPropertyWithValue("code", CartErrorCode.STORE_NOT_FOUND);
        }

        @Test
        @DisplayName("사용자 주소 조회 실패 시 UNAUTHORIZED")
        void user_address_fail() {
            var req = mkOrderCreateReq();
            Cart cart = mock(Cart.class);
            lenient().when(cart.getCartItems()).thenReturn(List.of(mock(CartItem.class)));
            lenient().when(cart.getStore()).thenReturn(storeId);
            when(cartRepository.findByIdAndUserWithCartItems(cartId, userId)).thenReturn(Optional.of(cart));
            when(cartRepository.existsByUserAndCart(userId, cartId)).thenReturn(true);
            when(userClient.addressById(userId)).thenThrow(new RuntimeException("x"));

            assertThatThrownBy(() -> service.createOrder(req, cartId, passport))
                    .isInstanceOf(OrderException.class)
                    .hasFieldOrPropertyWithValue("code", OrderErrorCode.UNAUTHORIZED_ACCESS);
        }
    }

    @Nested
    @DisplayName("updateOrder")
    class UpdateOrder {
        @Test
        @DisplayName("ORDER_CANCELED 변경 시 취소 이벤트 발행")
        void cancel_emits_event() {
            UUID orderId = UUID.randomUUID();
            when(orderRepository.existsByOrderIdAndUserId(orderId, userId)).thenReturn(true);

            Order order = mock(Order.class);
            when(order.getStatus()).thenReturn(OrderStatus.PENDING);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            var req = OrderRequestDTO.OrderUpdateRequestDTO.builder().newStatus(OrderStatus.ORDER_CANCELED).build();
            var resp = mock(OrderResponseDTO.OrderUpdateResponseDTO.class);

            try (MockedStatic<OrderConverter> oc = mockStatic(OrderConverter.class)) {
                oc.when(() -> OrderConverter.toOrderUpdateResponseDTO(eq(order), any(OrderStatus.class)))
                        .thenReturn(resp);

                var out = service.updateOrder(req, orderId, passport);
                assertThat(out).isSameAs(resp);
                verify(orderEventService).publishOrderCanceled(eq(order), anyString());
                verify(orderRepository).save(order);
            }
        }
    }

    @Nested
    @DisplayName("deleteOrder")
    class DeleteOrder {
        @Test
        @DisplayName("취소 상태가 아니면 cancel 이벤트 후 softDelete")
        void delete_emits_cancel_and_softDelete() {
            UUID orderId = UUID.randomUUID();
            when(orderRepository.existsByOrderIdAndUserId(orderId, userId)).thenReturn(true);

            Order order = mock(Order.class);
            when(order.getStatus()).thenReturn(OrderStatus.PENDING);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            service.deleteOrder(orderId, passport);

            verify(orderEventService).publishOrderCanceled(eq(order), anyString());
            verify(order).softDelete();
        }
    }
}
