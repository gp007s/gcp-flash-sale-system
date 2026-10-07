package com.gcp.flashsale.service;

import java.util.List;

import com.gcp.flashsale.model.OrderFulfillmentDTO;
import com.gcp.flashsale.model.OrderFulfillmentData;
import com.gcp.flashsale.repository.OrderFulfillmentRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/** Runs on Cloud Run (role "fulfilment"): saves the order in Cloud SQL and stores the receipt. */
@Service
@ConditionalOnProperty(name = "flashsale.role", havingValue = "fulfilment")
public class OrderFulfillmentService {

    private static final Logger log = LoggerFactory.getLogger(OrderFulfillmentService.class);

    private final OrderFulfillmentRepository repository;
    private final RedisService redisService;
    private final ReceiptService receiptService;

    public OrderFulfillmentService(
            OrderFulfillmentRepository repository,
            RedisService redisService,
            ReceiptService receiptService) {
        this.repository = repository;
        this.redisService = redisService;
        this.receiptService = receiptService;
    }

    /**
     * Returns true when the order is fulfilled (or was already fulfilled earlier).
     * Returns false when it failed; in that case the stock was given back to Redis.
     */
    public boolean processOrder(OrderFulfillmentDTO order) {
        log.info("Processing the order fulfillment for {}", order.getOrderId());

        try {
            // Pub/Sub can deliver a message more than once. Never fulfil the same order twice.
            if (repository.existsByOrderId(order.getOrderId())) {
                log.info("Order {} was already fulfilled, ignoring the duplicate", order.getOrderId());
                return true;
            }

            OrderFulfillmentData data = toEntity(order);
            data.setFulfillmentStatus("COMPLETED");
            repository.save(data);
        } catch (DataIntegrityViolationException duplicate) {
            // Two copies of the same message ran at the same time. The other one won.
            log.info("Order {} was saved by another delivery, ignoring the duplicate", order.getOrderId());
            return true;
        } catch (Exception e) {
            log.error("Order {} fulfillment failed, restoring the stock in Redis", order.getOrderId(), e);
            restoreStock(order);
            return false;
        }

        // The order is saved. The receipt is a bonus: a receipt problem must not fail the order.
        try {
            receiptService.createAndStoreReceipt(order);
        } catch (Exception e) {
            log.warn("Order {} is fulfilled but the receipt could not be stored", order.getOrderId(), e);
        }
        return true;
    }

    public List<OrderFulfillmentData> getAllOrderFulfillmentDetails() {
        return repository.findAll();
    }

    public OrderFulfillmentData orderFulfillmentDetails(Long id) {
        return repository.findById(id).orElse(null);
    }

    private void restoreStock(OrderFulfillmentDTO order) {
        try {
            redisService.increment(RedisService.stockKey(order.getItemDetails()), order.getQuantity());
        } catch (Exception e) {
            log.error("STOCK LEAK: could not give {} x {} back to Redis", order.getQuantity(), order.getItemDetails(), e);
        }
    }

    private static OrderFulfillmentData toEntity(OrderFulfillmentDTO dto) {
        OrderFulfillmentData data = new OrderFulfillmentData();
        data.setOrderId(dto.getOrderId());
        data.setItemId(dto.getItemId());
        data.setItemDetails(dto.getItemDetails());
        data.setQuantity(dto.getQuantity());
        data.setUserName(dto.getUserName());
        data.setEmailAddress(dto.getEmailAddress());
        data.setPaymentAmount(dto.getPaymentAmount());
        return data;
    }
}