package com.marketlane.shop;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CartService {
    private static final Duration HOLD_DURATION = Duration.ofMinutes(15);
    private final ProductRepository products;
    private final CartItemRepository cartItems;

    public CartService(ProductRepository products, CartItemRepository cartItems) {
        this.products = products;
        this.cartItems = cartItems;
    }

    @Transactional
    public CartResponse add(String userId, Long productId, int quantity) {
        requirePositive(quantity);
        Instant now = Instant.now();
        expireUser(userId, now);
        Product product = lockProduct(productId);
        if (product.getStock() < quantity) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough stock available");
        }
        product.setStock(product.getStock() - quantity);
        products.save(product);
        CartItem line = cartItems.findByUserIdAndProductId(userId, productId)
                .orElseGet(() -> new CartItem(userId, productId, 0, now.plus(HOLD_DURATION)));
        line.setQuantity(line.getQuantity() + quantity);
        line.setExpiresAt(now.plus(HOLD_DURATION));
        cartItems.save(line);
        return getCart(userId);
    }

    @Transactional
    public CartResponse update(String userId, Long productId, int quantity) {
        requirePositive(quantity);
        Instant now = Instant.now();
        expireUser(userId, now);
        CartItem line = cartItems.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));
        Product product = lockProduct(productId);
        int delta = quantity - line.getQuantity();
        if (delta > product.getStock()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough stock available");
        }
        product.setStock(product.getStock() - delta);
        products.save(product);
        line.setQuantity(quantity);
        line.setExpiresAt(now.plus(HOLD_DURATION));
        cartItems.save(line);
        return getCart(userId);
    }

    @Transactional
    public CartResponse remove(String userId, Long productId) {
        expireUser(userId, Instant.now());
        cartItems.findByUserIdAndProductId(userId, productId).ifPresent(line -> {
            release(line);
            cartItems.delete(line);
        });
        return getCart(userId);
    }

    @Transactional
    public CartResponse getCart(String userId) {
        expireUser(userId, Instant.now());
        List<CartLineResponse> lines = cartItems.findByUserId(userId).stream()
                .map(line -> new CartLineResponse(line.getId(),
                        products.findById(line.getProductId()).orElseThrow(),
                        line.getQuantity(), line.getExpiresAt()))
                .sorted(Comparator.comparing(item -> item.product().getName()))
                .toList();
        BigDecimal total = lines.stream()
                .map(item -> item.product().getPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Instant expiresAt = lines.stream().map(CartLineResponse::expiresAt).min(Instant::compareTo).orElse(null);
        return new CartResponse(lines, total, expiresAt);
    }

    @Transactional
    @Scheduled(fixedDelay = 60000)
    public void expireAll() {
        cartItems.findByExpiresAtBefore(Instant.now()).forEach(line -> {
            release(line);
            cartItems.delete(line);
        });
    }

    private void expireUser(String userId, Instant now) {
        cartItems.findByUserId(userId).stream()
                .filter(line -> !line.getExpiresAt().isAfter(now))
                .toList()
                .forEach(line -> {
                    release(line);
                    cartItems.delete(line);
                });
    }

    private void release(CartItem line) {
        products.lockById(line.getProductId()).ifPresent(product -> {
            product.setStock(product.getStock() + line.getQuantity());
            products.save(product);
        });
    }

    private Product lockProduct(Long productId) {
        return products.lockById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private void requirePositive(int quantity) {
        if (quantity < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be at least one");
        }
    }

    public record CartLineResponse(Long id, Product product, int quantity, Instant expiresAt) {}
    public record CartResponse(List<CartLineResponse> items, BigDecimal total, Instant expiresAt) {}
}