package com.marketlane.shop.config;

import com.marketlane.shop.model.FavoriteItem;
import com.marketlane.shop.model.Product;
import com.marketlane.shop.repository.FavoriteRepository;
import com.marketlane.shop.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {
    private final ProductRepository products;
    private final FavoriteRepository favorites;

    public DataSeeder(ProductRepository products, FavoriteRepository favorites) {
        this.products = products;
        this.favorites = favorites;
    }

    @Override
    public void run(String... args) {
        if (products.count() > 0) return;
        Instant now = Instant.now();
        List<Product> seeded = products.saveAll(List.of(
                product("Sunday ceramic pour-over", "A slow-morning essential, handmade in Hackney.", "Home", "34.00", "42.00", "photo-1495474472287-4d71bcdd2085", 18, 98, true, now.minus(1, ChronoUnit.DAYS)),
                product("Good Things market tote", "Room for the market, the bookshop, and one more thing.", "Accessories", "22.00", "22.00", "photo-1590874103328-eac38a683ce7", 24, 96, false, now.minus(40, ChronoUnit.DAYS)),
                product("Field notes candle", "Cedar, fig leaf and a hint of rain on warm pavement.", "Home", "28.00", "34.00", "photo-1603006905003-be475563bc59", 11, 92, true, now.minus(4, ChronoUnit.DAYS)),
                product("Studio everyday glass set", "Four light-catching glasses for everyday tables.", "Kitchen", "31.00", "31.00", "photo-1614887065001-06c958a7cddd", 20, 89, false, now.minus(63, ChronoUnit.DAYS)),
                product("Sunday market coffee", "Small-batch beans with chocolate and orange notes.", "Grocery", "16.50", "19.00", "photo-1445116572660-236099ec97a0", 34, 86, true, now.minus(2, ChronoUnit.DAYS)),
                product("Linen table runner", "Soft washed linen, made for long lunches.", "Home", "39.00", "39.00", "photo-1600210492486-724fe5c67fb0", 9, 81, false, now.minus(90, ChronoUnit.DAYS)),
                product("Pocket-sized city journal", "An unlined place for lists, routes and small plans.", "Stationery", "14.00", "17.00", "photo-1531346878377-a5be20888e57", 32, 78, true, now.minus(3, ChronoUnit.DAYS)),
                product("Botanical hand wash", "A bright, gentle wash blended in South London.", "Body", "19.00", "19.00", "photo-1608248543803-ba4f8c70ae0b", 15, 73, false, now.minus(22, ChronoUnit.DAYS))
        ));
        favorites.saveAll(List.of(new FavoriteItem("demo", seeded.get(0).getId()),
                new FavoriteItem("demo", seeded.get(2).getId()),
                new FavoriteItem("demo", seeded.get(4).getId())));
    }

    private Product product(String name, String description, String category, String price,
                            String compareAt, String photoId, int stock, int popularity,
                            boolean isNew, Instant createdAt) {
        return new Product(name, description, category, new BigDecimal(price),
                new BigDecimal(compareAt), "https://images.unsplash.com/" + photoId + "?auto=format&fit=crop&w=900&q=85",
                stock, popularity, isNew, createdAt);
    }
}