package com.marketlane.shop;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ShoppingApiIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private CartItemRepository cartItems;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void clearCart() {
        cartItems.deleteAll();
    }

    @Test
    void popularFeedIsAvailableAndShortLivedCacheable() throws Exception {
        mvc.perform(get("/api/dashboard/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").isNotEmpty())
                .andExpect(jsonPath("$[0].isNew").isBoolean())
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertTrue(
                        result.getResponse().getHeader("Cache-Control").contains("max-age=30")));
    }

    @Test
    void londonVouchersIncludeDiscountAndEligibilityTerms() throws Exception {
        mvc.perform(get("/api/dashboard/vouchers?location=London"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].discountType").value("PERCENT"))
                .andExpect(jsonPath("$[0].discountValue").value(10))
                .andExpect(jsonPath("$[0].minimumSpend").value(30))
                .andExpect(jsonPath("$[1].discountType").value("FIXED"))
                .andExpect(jsonPath("$[1].discountValue").value(5))
                .andExpect(jsonPath("$[1].minimumSpend").value(25));

        mvc.perform(get("/api/dashboard/vouchers?location=Manchester"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void cartCanBeAddedUpdatedAndRemoved() throws Exception {
        mvc.perform(post("/api/cart/items?userId=integration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", 1, "quantity", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].expiresAt").isNotEmpty());

        mvc.perform(put("/api/cart/items/1?userId=integration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("quantity", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(1));

        mvc.perform(delete("/api/cart/items/1?userId=integration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void rejectsReservationWhenQuantityIsNotPositive() throws Exception {
        mvc.perform(post("/api/cart/items?userId=integration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", 1, "quantity", 0))))
                .andExpect(status().isBadRequest());
    }
}