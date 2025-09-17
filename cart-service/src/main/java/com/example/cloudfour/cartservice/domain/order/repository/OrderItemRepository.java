package com.example.cloudfour.cartservice.domain.order.repository;

import com.example.cloudfour.cartservice.domain.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
    @Query("select oi from OrderItem oi where oi.order.id =:OrderId and oi.order.isDeleted = false")
    List<OrderItem> findByOrderId(@Param("OrderId") UUID orderId);

    @Query("select oi from OrderItem oi where oi.id =:orderItemId and oi.order.isDeleted = false")
    Optional<OrderItem> findById(@Param("orderItemId") UUID orderItemId);

    @Query("select count(oi) > 0 from OrderItem oi join oi.order o " +
           "where oi.id = :orderItemId and o.user = :userId and o.isDeleted = false")
    boolean existsByUserId(@Param("orderItemId") UUID orderItemId, @Param("userId") UUID userId);
}
