package com.gcp.flashsale.controller;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcp.flashsale.model.OrderFulfillmentDTO;
import com.gcp.flashsale.model.PubSubPushRequest;
import com.gcp.flashsale.service.OrderFulfillmentService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pub/Sub push subscription -> Cloud Run. This replaces the Lambda that read SQS in the AWS project.
 *
 * Pub/Sub retries on any non-2xx answer. So we answer 200 whenever we are DONE with the message:
 * fulfilled, failed (stock was given back), or unreadable (retrying cannot fix it).
 * Cloud Run checks the Pub/Sub identity token itself when the service is private.
 */
@RestController
@ConditionalOnProperty(name = "flashsale.role", havingValue = "fulfilment")
public class PubSubPushController {

    private static final Logger log = LoggerFactory.getLogger(PubSubPushController.class);

    private final OrderFulfillmentService service;
    private final ObjectMapper objectMapper;

    public PubSubPushController(OrderFulfillmentService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/pubsub/push")
    public ResponseEntity<String> receive(@RequestBody PubSubPushRequest push) {
        OrderFulfillmentDTO order;
        try {
            String json = new String(Base64.getDecoder().decode(push.message().data()), StandardCharsets.UTF_8);
            order = objectMapper.readValue(json, OrderFulfillmentDTO.class);
        } catch (Exception e) {
            log.warn("Unreadable Pub/Sub message, acknowledging it so it is not retried forever", e);
            return ResponseEntity.ok("IGNORED: unreadable message");
        }

        if (order.getOrderId() == null || order.getOrderId().isBlank()) {
            log.warn("Pub/Sub message without orderId, acknowledging it");
            return ResponseEntity.ok("IGNORED: no orderId");
        }

        boolean fulfilled = service.processOrder(order);
        return ResponseEntity.ok(fulfilled ? "FULFILLED" : "FAILED: stock restored");
    }
}