package com.marketlane.shop.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Constructor;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EntityCoverageTest {
    @Test
    void cartItemConstructorsAndAccessorsWork() throws ReflectiveOperationException {
        Instant expiresAt = Instant.now();
        CartItem item = new CartItem("demo", 1L, 2, expiresAt);
        assertEquals("demo", item.getUserId());
        assertEquals(1L, item.getProductId());
        assertEquals(2, item.getQuantity());
        assertEquals(expiresAt, item.getExpiresAt());
        item.setQuantity(3);
        item.setExpiresAt(expiresAt.plusSeconds(60));
        assertEquals(3, item.getQuantity());
        assertEquals(expiresAt.plusSeconds(60), item.getExpiresAt());

        Constructor<CartItem> noArgsConstructor = CartItem.class.getDeclaredConstructor();
        noArgsConstructor.setAccessible(true);
        assertNull(noArgsConstructor.newInstance().getId());
    }

    @Test
    void favoriteItemConstructorsAndAccessorsWork() throws ReflectiveOperationException {
        FavoriteItem item = new FavoriteItem("demo", 2L);
        assertEquals("demo", item.getUserId());
        assertEquals(2L, item.getProductId());

        Constructor<FavoriteItem> noArgsConstructor = FavoriteItem.class.getDeclaredConstructor();
        noArgsConstructor.setAccessible(true);
        assertNotNull(noArgsConstructor.newInstance());
    }
}
