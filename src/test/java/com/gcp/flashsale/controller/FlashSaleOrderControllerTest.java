package com.gcp.flashsale.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gcp.flashsale.service.PubSubService;
import com.gcp.flashsale.service.RedisService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(FlashSaleOrderController.class)
class FlashSaleOrderControllerTest {

    private static final String ORDER_JSON =
            "{\"itemName\":\"iphone\",\"quantity\":2,\"userName\":\"Govind\","
                    + "\"emailAddress\":\"g@example.com\",\"paymentAmount\":999}";

    @Autowired
    MockMvc mvc;

    @MockBean
    RedisService redisService;

    @MockBean
    PubSubService pubSubService;

    private ResultActions order(String json) throws Exception {
        return mvc.perform(post("/api/user/order").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void acceptsOrderAndSendsItToPubSub() throws Exception {
        when(redisService.checkItemAvailabilityAndReserve("stock:iphone", 2)).thenReturn(8L);
        when(pubSubService.sendMessage(any())).thenReturn("msg-1");

        order(ORDER_JSON)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("ORDER-COMPLETED"))
                .andExpect(jsonPath("$.fulfillmentStatus").value("PENDING"))
                .andExpect(jsonPath("$.orderNumber").isNotEmpty());

        verify(pubSubService).sendMessage(any());
    }

    @Test
    void buyingTheLastItemsStillWorks() throws Exception {
        // The AWS version treated "0 left" as out of stock. 0 left means we just sold the last ones.
        when(redisService.checkItemAvailabilityAndReserve("stock:iphone", 2)).thenReturn(0L);
        when(pubSubService.sendMessage(any())).thenReturn("msg-2");

        order(ORDER_JSON)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("ORDER-COMPLETED"));
    }

    @Test
    void reportsOutOfStockAndDoesNotPublish() throws Exception {
        when(redisService.checkItemAvailabilityAndReserve("stock:iphone", 2)).thenReturn(-1L);

        order(ORDER_JSON)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.orderStatus").value("OUT-OF-STOCK"));

        verifyNoInteractions(pubSubService);
    }

    @Test
    void givesStockBackWhenPubSubFails() throws Exception {
        when(redisService.checkItemAvailabilityAndReserve("stock:iphone", 2)).thenReturn(8L);
        when(pubSubService.sendMessage(any())).thenThrow(new RuntimeException("pubsub down"));

        order(ORDER_JSON)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.orderStatus").value("ORDER-FAILED"));

        verify(redisService).increment("stock:iphone", 2);
    }

    @Test
    void rejectsQuantityBelowOne() throws Exception {
        order("{\"itemName\":\"iphone\",\"quantity\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.orderStatus").value("ORDER-FAILED"));

        verifyNoInteractions(redisService);
    }
}