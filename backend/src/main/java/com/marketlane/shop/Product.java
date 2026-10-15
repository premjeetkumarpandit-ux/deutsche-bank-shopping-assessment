package com.marketlane.shop;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private String imageUrl;
    private int stock;
    private int popularity;
    private boolean isNew;
    private Instant createdAt;

    protected Product() {}

    public Product(String name, String description, String category, BigDecimal price,
                   BigDecimal compareAtPrice, String imageUrl, int stock, int popularity,
                   boolean isNew, Instant createdAt) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.compareAtPrice = compareAtPrice;
        this.imageUrl = imageUrl;
        this.stock = stock;
        this.popularity = popularity;
        this.isNew = isNew;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getCompareAtPrice() { return compareAtPrice; }
    public String getImageUrl() { return imageUrl; }
    public int getStock() { return stock; }
    public int getPopularity() { return popularity; }
    @JsonProperty("isNew")
    public boolean isNew() { return isNew; }
    public Instant getCreatedAt() { return createdAt; }
    public void setStock(int stock) { this.stock = stock; }
}