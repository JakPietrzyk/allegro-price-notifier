package com.priceprocessor.services;

import com.priceprocessor.dtos.api.ProductObservationRequest;
import com.priceprocessor.dtos.api.ProductObservationResponse;
import com.priceprocessor.dtos.crawler.PriceResponse;
import com.priceprocessor.models.ProductObservation;
import com.priceprocessor.repositories.ProductRepository;
import com.priceprocessor.services.interfaces.PriceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private PriceClient priceClient;

    @InjectMocks
    private ProductService productService;

    @Test
    void shouldStartObservingProductByName_WhenScraperReturnsData() {
        // Arrange
        String productName = "iPhone 13";
        String userEmail = "user@test.com";
        ProductObservationRequest request = new ProductObservationRequest(productName, null, userEmail);

        PriceResponse mockPriceResponse = new PriceResponse(
                "iPhone 13 128GB",
                3000.00,
                "PLN",
                "http://ceneo.pl/123"
        );

        ProductObservation savedEntity = ProductObservation.builder()
                .id(1L)
                .productName("iPhone 13 128GB")
                .currentPrice(3000.00)
                .productUrl("http://ceneo.pl/123")
                .build();

        when(priceClient.checkPriceByName(any(String.class)))
                .thenReturn(Optional.of(mockPriceResponse));

        when(productRepository.save(any(ProductObservation.class)))
                .thenReturn(savedEntity);

        // Act
        ProductObservationResponse result = productService.startObservingProductByName(request);

        // Assert
        assertThat(result.productName()).isEqualTo("iPhone 13 128GB");
        assertThat(result.currentPrice()).isEqualTo(3000.00);

        verify(priceClient).checkPriceByName(productName);
    }
}