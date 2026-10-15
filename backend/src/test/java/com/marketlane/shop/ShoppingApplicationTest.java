package com.marketlane.shop;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class ShoppingApplicationTest {
    @Test
    void mainDelegatesToSpringApplication() {
        String[] args = {"--spring.profiles.active=test"};
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            ShoppingApplication.main(args);

            springApplication.verify(() -> SpringApplication.run(ShoppingApplication.class, args));
        }
    }
}
