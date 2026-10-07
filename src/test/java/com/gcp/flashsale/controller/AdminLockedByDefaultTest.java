package com.gcp.flashsale.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gcp.flashsale.service.RedisService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/** No ADMIN_API_KEY configured: the admin endpoints must stay locked, even if someone sends a header. */
@WebMvcTest(controllers = ConfigureSaleStocksController.class, properties = "flashsale.admin-key=")
class AdminLockedByDefaultTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    RedisService redisService;

    @Test
    void lockedWhenNoKeyIsConfigured() throws Exception {
        mvc.perform(post("/api/admin/set-sale-stock").header("X-Admin-Key", "")
                        .param("itemNameInOffer", "iphone").param("maxItemQuantityOnSale", "5"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(redisService);
    }
}