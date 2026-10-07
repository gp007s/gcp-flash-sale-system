package com.gcp.flashsale.controller;

import java.util.List;

import com.gcp.flashsale.model.OrderFulfillmentDTO;
import com.gcp.flashsale.model.OrderFulfillmentData;
import com.gcp.flashsale.service.OrderFulfillmentService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fulfilment API on Cloud Run (role "fulfilment"). Same paths as the AWS project.
 * Keep the Cloud Run service private (no public access): it holds customer data.
 */
@RestController
@RequestMapping("/order/fulfillment")
@ConditionalOnProperty(name = "flashsale.role", havingValue = "fulfilment")
public class OrderFulfillmentController {

    private final OrderFulfillmentService service;

    public OrderFulfillmentController(OrderFulfillmentService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<OrderFulfillmentData>> getAllOrders() {
        return ResponseEntity.ok(service.getAllOrderFulfillmentDetails());
    }

    @PostMapping
    public ResponseEntity<String> create(@RequestBody OrderFulfillmentDTO order) {
        if (order.getOrderId() == null || order.getOrderId().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("orderId is required");
        }
        boolean fulfilled = service.processOrder(order);
        return fulfilled
                ? ResponseEntity.ok("Order fulfilled successfully.")
                : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Order could not be fulfilled.");
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderFulfillmentData> one(@PathVariable Long id) {
        OrderFulfillmentData data = service.orderFulfillmentDetails(id);
        return data == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(data);
    }
}