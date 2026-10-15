package com.marketlane.shop;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FavoriteService {
    private final ProductRepository products;
    private final FavoriteRepository favorites;

    public FavoriteService(ProductRepository products, FavoriteRepository favorites) {
        this.products = products;
        this.favorites = favorites;
    }

    @Transactional
    public void save(String userId, Long productId) {
        if (!products.existsById(productId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
        if (favorites.findByUserIdAndProductId(userId, productId).isEmpty()) {
            favorites.save(new FavoriteItem(userId, productId));
        }
    }

    @Transactional
    public void remove(String userId, Long productId) {
        favorites.findByUserIdAndProductId(userId, productId).ifPresent(favorites::delete);
    }
}