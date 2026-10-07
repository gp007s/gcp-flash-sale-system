package com.gcp.flashsale.repository;

import com.gcp.flashsale.model.OrderFulfillmentData;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderFulfillmentRepository extends JpaRepository<OrderFulfillmentData, Long> {

    boolean existsByOrderId(String orderId);
}