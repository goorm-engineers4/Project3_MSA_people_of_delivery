package com.example.cloudfour.cartservice.domain.order.service.query;

import com.example.cloudfour.cartservice.client.StoreClient;
import com.example.cloudfour.cartservice.client.UserClient;
import com.example.cloudfour.cartservice.commondto.StoreResponseDTO;
import com.example.cloudfour.cartservice.domain.order.converter.OrderConverter;
import com.example.cloudfour.cartservice.domain.order.converter.OrderItemConverter;
import com.example.cloudfour.cartservice.domain.order.dto.OrderItemResponseDTO;
import com.example.cloudfour.cartservice.domain.order.dto.OrderResponseDTO;
import com.example.cloudfour.cartservice.domain.order.entity.Order;
import com.example.cloudfour.cartservice.domain.order.entity.OrderItem;
import com.example.cloudfour.cartservice.domain.order.exception.OrderErrorCode;
import com.example.cloudfour.cartservice.domain.order.exception.OrderException;
import com.example.cloudfour.cartservice.domain.order.exception.OrderItemException;
import com.example.cloudfour.cartservice.domain.order.exception.OrderItemErrorCode;
import com.example.cloudfour.cartservice.domain.order.repository.OrderItemRepository;
import com.example.cloudfour.cartservice.domain.order.repository.OrderRepository;
import com.example.cloudfour.modulecommon.dto.Passport;
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
@DisplayName("OrderQueryService 단위 테스트")
class OrderQueryServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock StoreClient storeClient;
    @Mock UserClient userClient;

    @InjectMocks OrderQueryService service;

    UUID userId;
    UUID orderId;
    Passport passport;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        passport = Passport.builder().userId(userId).role("ROLE_CUSTOMER").build();
    }

    @Nested
    @DisplayName("getOrderById")
    class GetOrderById {
        @Test
        @DisplayName("권한/소유권 확인 후 상세 반환")
        void success() {
            when(orderRepository.existsByOrderIdAndUserId(orderId, userId)).thenReturn(true);

            Order order = mock(Order.class);
            when(order.getStore()).thenReturn(UUID.randomUUID());
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(mock(OrderItem.class)));
            when(storeClient.storeById(any(UUID.class))).thenReturn(StoreResponseDTO.builder().name("가게").build());

            OrderItemResponseDTO.OrderItemListResponseDTO itemDto = mock(OrderItemResponseDTO.OrderItemListResponseDTO.class);
            OrderResponseDTO.OrderDetailResponseDTO resp = mock(OrderResponseDTO.OrderDetailResponseDTO.class);

            try (MockedStatic<OrderItemConverter> oic = mockStatic(OrderItemConverter.class);
                 MockedStatic<OrderConverter> oc = mockStatic(OrderConverter.class)) {
                oic.when(() -> OrderItemConverter.toOrderItemClassListDTO(any(OrderItem.class)))
                        .thenReturn(itemDto);
                oc.when(() -> OrderConverter.toOrderDetailResponseDTO(eq(order), anyList(), eq("가게")))
                        .thenReturn(resp);

                var out = service.getOrderById(orderId, passport);
                assertThat(out).isSameAs(resp);
            }
        }

        @Test
        @DisplayName("passport null이면 UNAUTHORIZED")
        void unauthorized_null_passport() {
            assertThatThrownBy(() -> service.getOrderById(orderId, null))
                    .isInstanceOf(OrderException.class)
                    .hasFieldOrPropertyWithValue("code", OrderErrorCode.UNAUTHORIZED_ACCESS);
        }
    }

    @Nested
    @DisplayName("getOrderItemById")
    class GetOrderItemById {
        @Test
        @DisplayName("소유자이면 주문 아이템 상세 반환")
        void owner_can_get_order_item() {
            UUID orderItemId = UUID.randomUUID();

            OrderItem orderItem = mock(OrderItem.class);
            when(orderItem.getId()).thenReturn(orderItemId);
            when(orderItemRepository.findById(orderItemId)).thenReturn(Optional.of(orderItem));
            when(orderItemRepository.existsByUserId(orderItemId, userId)).thenReturn(true);

            OrderItemResponseDTO.OrderItemListResponseDTO dto = mock(OrderItemResponseDTO.OrderItemListResponseDTO.class);

            try (MockedStatic<OrderItemConverter> oic = mockStatic(OrderItemConverter.class)) {
                oic.when(() -> OrderItemConverter.toOrderItemClassListDTO(orderItem))
                        .thenReturn(dto);

                var out = service.getOrderItemById(orderItemId, passport);
                assertThat(out).isSameAs(dto);
                verify(orderItemRepository).findById(orderItemId);
                verify(orderItemRepository).existsByUserId(orderItemId, userId);
            }
        }

        @Test
        @DisplayName("비소유자는 UNAUTHORIZED 예외")
        void non_owner_unauthorized() {
            UUID orderItemId = UUID.randomUUID();

            OrderItem orderItem = mock(OrderItem.class);
            when(orderItem.getId()).thenReturn(orderItemId);
            when(orderItemRepository.findById(orderItemId)).thenReturn(Optional.of(orderItem));
            when(orderItemRepository.existsByUserId(orderItemId, userId)).thenReturn(false);

            assertThatThrownBy(() -> service.getOrderItemById(orderItemId, passport))
                    .isInstanceOf(OrderItemException.class)
                    .hasFieldOrPropertyWithValue("code", OrderItemErrorCode.UNAUTHORIZED_ACCESS);

            verify(orderItemRepository).findById(orderItemId);
            verify(orderItemRepository).existsByUserId(orderItemId, userId);
        }

        @Test
        @DisplayName("존재하지 않으면 NOT_FOUND 예외")
        void not_found() {
            UUID orderItemId = UUID.randomUUID();
            when(orderItemRepository.findById(orderItemId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getOrderItemById(orderItemId, passport))
                    .isInstanceOf(OrderItemException.class)
                    .hasFieldOrPropertyWithValue("code", OrderItemErrorCode.NOT_FOUND);

            verify(orderItemRepository).findById(orderItemId);
            verify(orderItemRepository, never()).existsByUserId(any(), any());
        }
    }
}
