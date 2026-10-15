package com.marketlane.shop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import com.marketlane.shop.model.CartItem;
import com.marketlane.shop.model.Product;
import com.marketlane.shop.repository.CartItemRepository;
import com.marketlane.shop.repository.ProductRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void addRejectsNonPositiveQuantities(int quantity) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.add("demo", 1L, quantity));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(products, never()).lockById(1L);
    }

    @Test
    void addRejectsMissingProduct() {
        when(cartItems.findByUserId("demo")).thenReturn(List.of());
        when(products.lockById(1L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.add("demo", 1L, 1));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(products, never()).save(any(Product.class));
    }

    @Test
    void addRejectsInsufficientStock() {
        Product product = productWithStock(1);
        when(cartItems.findByUserId("demo")).thenReturn(List.of());
        when(products.lockById(1L)).thenReturn(Optional.of(product));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.add("demo", 1L, 2));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(products, never()).save(any(Product.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void updateRejectsNonPositiveQuantities(int quantity) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.update("demo", 1L, quantity));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void updateRejectsMissingCartItem() {
        when(cartItems.findByUserId("demo")).thenReturn(List.of());
        when(cartItems.findByUserIdAndProductId("demo", 1L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.update("demo", 1L, 1));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(products, never()).lockById(1L);
    }

    @Test
    void updateRejectsInsufficientStock() {
        CartItem line = new CartItem("demo", 1L, 2, Instant.now().plusSeconds(60));
        when(cartItems.findByUserId("demo")).thenReturn(List.of());
        when(cartItems.findByUserIdAndProductId("demo", 1L)).thenReturn(Optional.of(line));
        when(products.lockById(1L)).thenReturn(Optional.of(productWithStock(0)));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.update("demo", 1L, 3));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(cartItems, never()).save(any(CartItem.class));
    }

    @Test
    void removeReleasesReservedStockAndDeletesItem() {
        CartItem line = new CartItem("demo", 1L, 2, Instant.now().plusSeconds(60));
        Product product = productWithStock(3);
        when(cartItems.findByUserId("demo")).thenReturn(List.of());
        when(cartItems.findByUserIdAndProductId("demo", 1L)).thenReturn(Optional.of(line));
        when(products.lockById(1L)).thenReturn(Optional.of(product));

        service.remove("demo", 1L);

        assertEquals(5, product.getStock());
        verify(products).save(product);
        verify(cartItems).delete(line);
    }

    @Test
    void getCartCalculatesTotalAndEarliestExpiry() {
        Instant firstExpiry = Instant.now().plusSeconds(60);
        Instant secondExpiry = firstExpiry.plusSeconds(60);
        Product tea = new Product("Tea", "Tea", "Grocery", BigDecimal.valueOf(5), BigDecimal.valueOf(5),
                "image", 2, 1, false, Instant.now());
        Product mug = new Product("Mug", "Mug", "Home", BigDecimal.TEN, BigDecimal.TEN,
                "image", 2, 1, false, Instant.now());
        CartItem teaLine = new CartItem("demo", 1L, 2, secondExpiry);
        CartItem mugLine = new CartItem("demo", 2L, 1, firstExpiry);
        when(cartItems.findByUserId("demo")).thenReturn(List.of(teaLine, mugLine));
        when(products.findById(1L)).thenReturn(Optional.of(tea));
        when(products.findById(2L)).thenReturn(Optional.of(mug));

        CartService.CartResponse response = service.getCart("demo");

        assertEquals(List.of("Mug", "Tea"), response.items().stream()
                .map(item -> item.product().getName()).toList());
        assertEquals(BigDecimal.valueOf(20), response.total());
        assertEquals(firstExpiry, response.expiresAt());
    }

        @Test
        void getCartExpiresStaleItemsAndKeepsActiveItems() {
                Instant now = Instant.now();
                CartItem expiredLine = new CartItem("demo", 1L, 2, now.minusSeconds(60));
                CartItem activeLine = new CartItem("demo", 2L, 1, now.plusSeconds(60));
                Product expiredProduct = productWithStock(3);
                Product activeProduct = new Product("Mug", "Ceramic", "Home", BigDecimal.TEN, BigDecimal.TEN,
                                "image", 2, 1, false, now);
                when(cartItems.findByUserId("demo")).thenReturn(List.of(expiredLine, activeLine), List.of(activeLine));
                when(products.lockById(1L)).thenReturn(Optional.of(expiredProduct));
                when(products.findById(2L)).thenReturn(Optional.of(activeProduct));

                CartService.CartResponse response = service.getCart("demo");

                assertEquals(1, response.items().size());
                assertEquals("Mug", response.items().get(0).product().getName());
                assertEquals(5, expiredProduct.getStock());
                verify(products).save(expiredProduct);
                verify(cartItems).delete(expiredLine);
        }

        @Test
        void expireAllReleasesStockAndDeletesExpiredItems() {
                CartItem expiredLine = new CartItem("demo", 1L, 2, Instant.now().minusSeconds(60));
                Product product = productWithStock(3);
                when(cartItems.findByExpiresAtBefore(any(Instant.class))).thenReturn(List.of(expiredLine));
                when(products.lockById(1L)).thenReturn(Optional.of(product));

                service.expireAll();

                assertEquals(5, product.getStock());
                verify(products).save(product);
                verify(cartItems).delete(expiredLine);
        }

    private Product productWithStock(int stock) {
        return new Product("Cup", "Ceramic", "Home", BigDecimal.TEN, BigDecimal.TEN,
                "image", stock, 1, false, Instant.now());
    }
}