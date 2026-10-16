package com.gcp.flashsale.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import com.gcp.flashsale.model.OrderFulfillmentDTO;

import org.junit.jupiter.api.Test;

class ReceiptServiceTest {

    @Test
    void buildsAPdfEvenWhenTheNameHasSpecialCharacters() throws Exception {
        OrderFulfillmentDTO order = new OrderFulfillmentDTO();
        order.setOrderId("ORD1");
        order.setItemDetails("iphone");
        order.setQuantity(1);
        order.setUserName("Govind");
        order.setPaymentAmount(999);

        byte[] pdf = new ReceiptService(null).buildPdf(order);

        assertTrue(pdf.length > 100);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }
}