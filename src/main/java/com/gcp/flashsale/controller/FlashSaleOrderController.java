package com.gcp.flashsale.controller;

import java.util.Random;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcp.flashsale.model.OrderFulfillmentDTO;
import com.gcp.flashsale.model.PurchaseRequest;
import com.gcp.flashsale.model.PurchaseResponse;
import com.gcp.flashsale.service.PubSubService;
import com.gcp.flashsale.service.RedisService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User side of the diagram (runs on the Compute Engine VMs).
 * Sync part:  reserve the stock in Memorystore (stock = stock - quantity).
 * Async part: publish the order to Pub/Sub, Cloud Run does the fulfilment.
 */
@RestController
@RequestMapping("/api/user")
@ConditionalOnProperty(name = "flashsale.role", havingValue = "api", matchIfMissing = true)
public class FlashSaleOrderController {

    private static final Logger log = LoggerFactory.getLogger(FlashSaleOrderController.class);

    private final RedisService redisService;
    private final PubSubService pubSubService;
    private final ObjectMapper objectMapper;

    public FlashSaleOrderController(RedisService redisService, PubSubService pubSubService, ObjectMapper objectMapper) {
        this.redisService = redisService;
        this.pubSubService = pubSubService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/order")
    public ResponseEntity<PurchaseResponse> orderItem(@RequestBody PurchaseRequest purchaseRequest) {
        PurchaseResponse response = new PurchaseResponse();
        String itemName = purchaseRequest.getItemName();
        int quantity = purchaseRequest.getQuantity();

        if (itemName == null || itemName.isBlank() || quantity < 1) {
            response.setOrderStatus("ORDER-FAILED");
            response.setError("itemName and a quantity of at least 1 are required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        log.info("Customer ordering: {} x {}", quantity, itemName);

        String redisKey = RedisService.stockKey(itemName);
        long remaining;
        try {
            remaining = redisService.checkItemAvailabilityAndReserve(redisKey, quantity);
        } catch (Exception e) {
            log.error("Stock service (Redis) is not available", e);
            response.setOrderStatus("ORDER-FAILED");
            response.setError("Stock service is not available, please try again");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }

        // 0 means we just sold the last items. Only a negative number means sold out.
        if (remaining < 0) {
            log.info("Item {} is out of stock", itemName);
            response.setOrderStatus("OUT-OF-STOCK");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        OrderFulfillmentDTO order = new OrderFulfillmentDTO();
        order.setOrderId("ORD" + UUID.randomUUID());
        order.setItemId("ITM" + new Random().nextInt(Integer.MAX_VALUE));
        order.setItemDetails(itemName);
        order.setQuantity(quantity);
        order.setEmailAddress(purchaseRequest.getEmailAddress());
        order.setUserName(purchaseRequest.getUserName());
        order.setPaymentAmount(purchaseRequest.getPaymentAmount());

        try {
            // ASYNC call: Pub/Sub -> Cloud Run starts the fulfilment.
            String messageId = pubSubService.sendMessage(objectMapper.writeValueAsString(order));
            log.info("Order {} placed, Pub/Sub messageId={}", order.getOrderId(), messageId);
        } catch (Exception e) {
            // "failed -> stock = stock + quantity" from the diagram. Give the items back.
            log.error("Could not queue order {}, returning the stock", order.getOrderId(), e);
            giveStockBack(redisKey, quantity);
            response.setOrderStatus("ORDER-FAILED");
            response.setError("Could not queue the order, please try again");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }

        response.setOrderStatus("ORDER-COMPLETED");
        response.setFulfillmentStatus("PENDING");
        response.setOrderNumber(order.getOrderId());
        return ResponseEntity.ok(response);
    }

    private void giveStockBack(String redisKey, int quantity) {
        try {
            redisService.increment(redisKey, quantity);
        } catch (Exception e) {
            log.error("STOCK LEAK: could not give {} items back to {}", quantity, redisKey, e);
        }
    }
}