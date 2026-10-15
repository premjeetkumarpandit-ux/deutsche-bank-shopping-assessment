package com.marketlane.shop.config;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.marketlane.shop.repository.FavoriteRepository;
import com.marketlane.shop.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {
    @Mock private ProductRepository products;
    @Mock private FavoriteRepository favorites;
    @InjectMocks private DataSeeder seeder;

    @Test
    void doesNotSeedWhenProductsAlreadyExist() {
        when(products.count()).thenReturn(1L);

        seeder.run();

        verify(products, never()).saveAll(anyList());
        verifyNoInteractions(favorites);
    }
}
