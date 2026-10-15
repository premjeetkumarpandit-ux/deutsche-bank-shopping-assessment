package com.marketlane.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {
    @Mock private ProductRepository products;
    @Mock private CartItemRepository cartItems;
    @InjectMocks private CartService service;

    @Test
    void addReservesStockAndPersistsFifteenMinuteHold() {
        Product product = new Product("Cup", "Ceramic", "Home", BigDecimal.TEN, BigDecimal.TEN,
                "image", 5, 1, false, Instant.now());
        when(cartItems.findByUserId("demo")).thenReturn(List.of());
        when(products.lockById(1L)).thenReturn(Optional.of(product));
        when(cartItems.findByUserIdAndProductId("demo", 1L)).thenReturn(Optional.empty());
        when(cartItems.save(any(CartItem.class))).thenAnswer(call -> call.getArgument(0));
        when(cartItems.findByUserId("demo")).thenReturn(List.of());

        service.add("demo", 1L, 2);

        assertEquals(3, product.getStock());
        verify(products).save(product);
        verify(cartItems).save(org.mockito.ArgumentMatchers.argThat(line ->
                line.getQuantity() == 2 && line.getExpiresAt().isAfter(Instant.now().plusSeconds(14 * 60))));
    }
}