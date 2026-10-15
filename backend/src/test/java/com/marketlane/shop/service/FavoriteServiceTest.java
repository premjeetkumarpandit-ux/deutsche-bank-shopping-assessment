package com.marketlane.shop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketlane.shop.model.FavoriteItem;
import com.marketlane.shop.repository.FavoriteRepository;
import com.marketlane.shop.repository.ProductRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {
    @Mock private ProductRepository products;
    @Mock private FavoriteRepository favorites;
    @InjectMocks private FavoriteService service;

    @Test
    void saveCreatesFavoriteWhenProductExists() {
        when(products.existsById(1L)).thenReturn(true);
        when(favorites.findByUserIdAndProductId("demo", 1L)).thenReturn(Optional.empty());

        service.save("demo", 1L);

        verify(favorites).save(any(FavoriteItem.class));
    }

    @Test
    void saveDoesNotCreateDuplicateFavorite() {
        when(products.existsById(1L)).thenReturn(true);
        when(favorites.findByUserIdAndProductId("demo", 1L))
                .thenReturn(Optional.of(new FavoriteItem("demo", 1L)));

        service.save("demo", 1L);

        verify(favorites, never()).save(any(FavoriteItem.class));
    }

    @Test
    void saveRejectsUnknownProduct() {
        when(products.existsById(1L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.save("demo", 1L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(favorites, never()).findByUserIdAndProductId("demo", 1L);
    }

    @Test
    void removeDeletesExistingFavorite() {
        FavoriteItem favorite = new FavoriteItem("demo", 1L);
        when(favorites.findByUserIdAndProductId("demo", 1L)).thenReturn(Optional.of(favorite));

        service.remove("demo", 1L);

        verify(favorites).delete(favorite);
    }

    @Test
    void removeDoesNothingWhenFavoriteDoesNotExist() {
        when(favorites.findByUserIdAndProductId("demo", 1L)).thenReturn(Optional.empty());

        service.remove("demo", 1L);

        verify(favorites, never()).delete(any(FavoriteItem.class));
    }
}
