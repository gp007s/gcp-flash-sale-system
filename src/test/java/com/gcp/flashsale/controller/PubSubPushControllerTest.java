package com.gcp.flashsale.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.gcp.flashsale.service.OrderFulfillmentService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = PubSubPushController.class, properties = "flashsale.role=fulfilment")
class PubSubPushControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    OrderFulfillmentService service;

    private ResultActions push(String data) throws Exception {
        String envelope = "{\"message\":{\"data\":\"" + data + "\",\"messageId\":\"1\"},\"subscription\":\"s\"}";
        return mvc.perform(post("/pubsub/push").contentType(MediaType.APPLICATION_JSON).content(envelope));
    }

    private static String base64(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void fulfilsTheOrderInTheMessage() throws Exception {
        when(service.processOrder(any())).thenReturn(true);

        push(base64("{\"orderId\":\"ORD1\",\"itemDetails\":\"iphone\",\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(content().string("FULFILLED"));

        verify(service).processOrder(argThat(o -> "ORD1".equals(o.getOrderId()) && o.getQuantity() == 2));
    }

    @Test
    void acknowledgesButReportsAFailedFulfilment() throws Exception {
        when(service.processOrder(any())).thenReturn(false);

        push(base64("{\"orderId\":\"ORD2\",\"itemDetails\":\"iphone\",\"quantity\":1}"))
                .andExpect(status().isOk())
                .andExpect(content().string("FAILED: stock restored"));
    }

    @Test
    void acknowledgesUnreadableMessagesWithoutProcessing() throws Exception {
        push(base64("this is not json")).andExpect(status().isOk());
        verifyNoInteractions(service);
    }
}