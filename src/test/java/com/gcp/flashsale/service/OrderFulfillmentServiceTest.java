package com.gcp.flashsale.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcp.flashsale.model.OrderFulfillmentDTO;
import com.gcp.flashsale.model.OrderFulfillmentData;
import com.gcp.flashsale.repository.OrderFulfillmentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class OrderFulfillmentServiceTest {

    @Mock
    OrderFulfillmentRepository repository;

    @Mock
    RedisService redisService;

    @Mock
    ReceiptService receiptService;

    OrderFulfillmentService service;
    OrderFulfillmentDTO order;

    @BeforeEach
    void setUp() {
        service = new OrderFulfillmentService(repository, redisService, receiptService);
        order = new OrderFulfillmentDTO();
        order.setOrderId("ORD1");
        order.setItemDetails("iphone");
        order.setQuantity(2);
    }

    @Test
    void savesTheOrderAndStoresTheReceipt() throws Exception {
        when(repository.existsByOrderId("ORD1")).thenReturn(false);

        assertTrue(service.processOrder(order));

        verify(repository).save(any(OrderFulfillmentData.class));
        verify(receiptService).createAndStoreReceipt(order);
        verify(redisService, never()).increment(any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void givesStockBackWhenTheDatabaseFails() throws Exception {
        when(repository.existsByOrderId("ORD1")).thenReturn(false);
        when(repository.save(any())).thenThrow(new RuntimeException("db down"));

        assertFalse(service.processOrder(order));

        // The AWS version called decrement here, which took even MORE stock. We add it back.
        verify(redisService).increment("stock:iphone", 2);
        verify(receiptService, never()).createAndStoreReceipt(any());
    }

    @Test
    void ignoresAnOrderThatWasAlreadyFulfilled() throws Exception {
        when(repository.existsByOrderId("ORD1")).thenReturn(true);

        assertTrue(service.processOrder(order));

        verify(repository, never()).save(any());
        verify(redisService, never()).increment(any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void doesNotGiveStockBackForADuplicateThatRacedUs() throws Exception {
        when(repository.existsByOrderId("ORD1")).thenReturn(false);
        when(repository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate orderId"));

        assertTrue(service.processOrder(order));

        verify(redisService, never()).increment(any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void orderStaysFulfilledWhenTheReceiptFails() throws Exception {
        when(repository.existsByOrderId("ORD1")).thenReturn(false);
        doThrow(new RuntimeException("bucket down")).when(receiptService).createAndStoreReceipt(any());

        assertTrue(service.processOrder(order));

        verify(redisService, never()).increment(any(), org.mockito.ArgumentMatchers.anyInt());
    }
}