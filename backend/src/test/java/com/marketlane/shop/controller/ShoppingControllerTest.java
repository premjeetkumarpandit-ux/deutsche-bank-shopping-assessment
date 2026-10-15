package com.marketlane.shop.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketlane.shop.model.FavoriteItem;
import com.marketlane.shop.model.Product;
import com.marketlane.shop.repository.FavoriteRepository;
import com.marketlane.shop.repository.ProductRepository;
import com.marketlane.shop.service.CartService;
import com.marketlane.shop.service.FavoriteService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ShoppingControllerTest {
    @Mock private ProductRepository products;
    @Mock private FavoriteRepository favorites;
    @Mock private FavoriteService favoriteService;
    @Mock private CartService cartService;
    @InjectMocks private ShoppingController controller;

    @Test
    void likedReturnsExistingProductsAndSkipsOrphanedFavorites() {
        Product product = new Product("Cup", "Ceramic", "Home", BigDecimal.TEN, BigDecimal.TEN,
                "image", 3, 1, false, Instant.now());
        when(favorites.findByUserId("demo")).thenReturn(List.of(
                new FavoriteItem("demo", 1L), new FavoriteItem("demo", 2L)));
        when(products.findById(1L)).thenReturn(Optional.of(product));
        when(products.findById(2L)).thenReturn(Optional.empty());

        assertEquals(List.of(product), controller.liked("demo"));
    }

    @Test
    void saveFavoriteDelegatesAndReturnsSavedState() {
        assertEquals(Map.of("saved", true), controller.saveFavorite("demo", 1L));

        verify(favoriteService).save("demo", 1L);
    }

    @Test
    void removeFavoriteDelegatesAndReturnsRemovedState() {
        assertEquals(Map.of("saved", false), controller.removeFavorite("demo", 1L));

        verify(favoriteService).remove("demo", 1L);
    }

    @Test
    void cartDelegatesToCartService() {
        CartService.CartResponse response = new CartService.CartResponse(List.of(), BigDecimal.ZERO, null);
        when(cartService.getCart("demo")).thenReturn(response);

        assertEquals(response, controller.cart("demo"));
    }

    @ParameterizedTest
    @MethodSource("invalidAddRequests")
    void addRejectsRequestsMissingProductOrQuantity(ShoppingController.AddItemRequest request) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.add("demo", request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    static Stream<ShoppingController.AddItemRequest> invalidAddRequests() {
        return Stream.of(new ShoppingController.AddItemRequest(null, 1),
                new ShoppingController.AddItemRequest(1L, null));
    }

    @Test
    void updateRejectsRequestMissingQuantity() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.update("demo", 1L, new ShoppingController.UpdateItemRequest(null)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
}
