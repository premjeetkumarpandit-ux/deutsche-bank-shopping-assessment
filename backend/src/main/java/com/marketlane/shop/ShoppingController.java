package com.marketlane.shop;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ShoppingController {
    private final ProductRepository products;
    private final FavoriteRepository favorites;
    private final FavoriteService favoriteService;
    private final CartService cartService;

    public ShoppingController(ProductRepository products, FavoriteRepository favorites,
                              FavoriteService favoriteService, CartService cartService) {
        this.products = products;
        this.favorites = favorites;
        this.favoriteService = favoriteService;
        this.cartService = cartService;
    }

    @GetMapping("/dashboard/liked")
    public List<Product> liked(@RequestParam(defaultValue = "demo") String userId) {
        return favorites.findByUserId(userId).stream()
                .map(favorite -> products.findById(favorite.getProductId()).orElse(null))
                .filter(product -> product != null)
                .toList();
    }

    @PutMapping("/favorites/{productId}")
    public Map<String, Boolean> saveFavorite(@RequestParam(defaultValue = "demo") String userId,
                                              @PathVariable Long productId) {
        favoriteService.save(userId, productId);
        return Map.of("saved", true);
    }

    @DeleteMapping("/favorites/{productId}")
    public Map<String, Boolean> removeFavorite(@RequestParam(defaultValue = "demo") String userId,
                                                @PathVariable Long productId) {
        favoriteService.remove(userId, productId);
        return Map.of("saved", false);
    }

    @GetMapping("/dashboard/popular")
    public ResponseEntity<List<Product>> popular() {
        return cached(products.findTop12ByOrderByPopularityDesc());
    }

    @GetMapping("/dashboard/new")
    public ResponseEntity<List<Product>> newItems() {
        return cached(products.findTop12ByIsNewTrueOrderByCreatedAtDesc());
    }

    @GetMapping("/dashboard/vouchers")
    public List<Voucher> vouchers(@RequestParam(defaultValue = "London") String location) {
        if (!"London".equalsIgnoreCase(location)) return List.of();
        Instant now = Instant.now();
        return List.of(
            new Voucher("WELCOME10", "A little something off", "10% off orders over £30", "10% off",
                DiscountType.PERCENT, new BigDecimal("10"), new BigDecimal("30.00"), now.plus(5, ChronoUnit.HOURS)),
            new Voucher("LOCAL5", "London locals", "£5 off orders over £25", "£5 off",
                DiscountType.FIXED, new BigDecimal("5.00"), new BigDecimal("25.00"), now.plus(2, ChronoUnit.DAYS)));
    }

    @GetMapping("/cart")
    public CartService.CartResponse cart(@RequestParam(defaultValue = "demo") String userId) {
        return cartService.getCart(userId);
    }

    @PostMapping("/cart/items")
    public CartService.CartResponse add(@RequestParam(defaultValue = "demo") String userId,
                                       @Valid @RequestBody AddItemRequest request) {
        if (request.quantity() == null || request.productId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product and quantity are required");
        }
        return cartService.add(userId, request.productId(), request.quantity());
    }

    @PutMapping("/cart/items/{productId}")
    public CartService.CartResponse update(@RequestParam(defaultValue = "demo") String userId,
                                           @PathVariable Long productId,
                                           @Valid @RequestBody UpdateItemRequest request) {
        if (request.quantity() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity is required");
        }
        return cartService.update(userId, productId, request.quantity());
    }

    @DeleteMapping("/cart/items/{productId}")
    public CartService.CartResponse remove(@RequestParam(defaultValue = "demo") String userId,
                                           @PathVariable Long productId) {
        return cartService.remove(userId, productId);
    }

    private ResponseEntity<List<Product>> cached(List<Product> products) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS).cachePublic())
                .body(products);
    }

    public record AddItemRequest(@NotNull Long productId, @NotNull @Min(1) Integer quantity) {}
    public record UpdateItemRequest(@NotNull @Min(1) Integer quantity) {}
    public enum DiscountType { PERCENT, FIXED }
    public record Voucher(String code, String title, String description, String discountLabel,
                          DiscountType discountType, BigDecimal discountValue,
                          BigDecimal minimumSpend, Instant validUntil) {}
}