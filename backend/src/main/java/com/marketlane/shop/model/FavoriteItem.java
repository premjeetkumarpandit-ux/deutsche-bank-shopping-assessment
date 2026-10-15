package com.marketlane.shop.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "favorites", uniqueConstraints = {
    @UniqueConstraint(name = "uk_favorite_user_product", columnNames = {"userId", "productId"})
})
public class FavoriteItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userId;
    private Long productId;

    protected FavoriteItem() {}
    public FavoriteItem(String userId, Long productId) {
        this.userId = userId;
        this.productId = productId;
    }
    public String getUserId() { return userId; }
    public Long getProductId() { return productId; }
}