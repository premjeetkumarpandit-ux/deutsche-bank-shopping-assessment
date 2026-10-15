package com.marketlane.shop.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "cart_items", indexes = {
        @Index(name = "idx_cart_user_expiry", columnList = "userId,expiresAt"),
        @Index(name = "idx_cart_product", columnList = "productId")
})
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userId;
    private Long productId;
    private int quantity;
    private Instant expiresAt;

    protected CartItem() {}
    public CartItem(String userId, Long productId, int quantity, Instant expiresAt) {
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.expiresAt = expiresAt;
    }
    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public Long getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}