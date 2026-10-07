package com.gcp.flashsale.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gcp.flashsale.service.RedisService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ConfigureSaleStocksController.class, properties = "flashsale.admin-key=secret")
class AdminKeyFilterTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    RedisService redisService;

    @Test
    void rejectsRequestWithoutKey() throws Exception {
        mvc.perform(post("/api/admin/set-sale-stock")
                        .param("itemNameInOffer", "iphone").param("maxItemQuantityOnSale", "5"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(redisService);
    }

    @Test
    void rejectsWrongKey() throws Exception {
        mvc.perform(post("/api/admin/set-sale-stock").header("X-Admin-Key", "nope")
                        .param("itemNameInOffer", "iphone").param("maxItemQuantityOnSale", "5"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(redisService);
    }

    @Test
    void setsStockWithTheRightKey() throws Exception {
        mvc.perform(post("/api/admin/set-sale-stock").header("X-Admin-Key", "secret")
                        .param("itemNameInOffer", "iphone").param("maxItemQuantityOnSale", "5"))
                .andExpect(status().isOk());
        verify(redisService).loadTheKeyValueFirstTime("stock:iphone", 5);
    }
}