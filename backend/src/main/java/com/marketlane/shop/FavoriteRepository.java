package com.marketlane.shop;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<FavoriteItem, Long> {
    List<FavoriteItem> findByUserId(String userId);
    Optional<FavoriteItem> findByUserIdAndProductId(String userId, Long productId);
}