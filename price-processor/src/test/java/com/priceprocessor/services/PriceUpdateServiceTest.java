package com.priceprocessor.services;

import com.priceprocessor.dtos.crawler.PriceResponse;
import com.priceprocessor.models.ProductObservation;
import com.priceprocessor.repositories.ProductRepository;
import com.priceprocessor.services.clients.PriceClient;
import com.priceprocessor.services.queue.NotificationProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceUpdateServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private PriceClient priceClient;
    @Mock
    private NotificationProducer notificationProducer;
    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private PriceUpdateService priceUpdateService;

    @Test
    void shouldReturnZero_WhenNoProductsToUpdate() {
        // Arrange
        when(productRepository.findProductsToUpdate(any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        // Act
        int count = priceUpdateService.updateOutdatedPrices();

        // Assert
        assertThat(count).isZero();
        verifyNoInteractions(priceClient);
        verifyNoInteractions(notificationProducer);
    }

    @Test
    void shouldUpdatePriceAndNotify_WhenPriceDrops() {
        // Arrange
        BigDecimal oldPrice = new BigDecimal("100.00");
        BigDecimal newPrice = new BigDecimal("80.00"); // Taniej!

        ProductObservation product = createProduct(oldPrice);
        PriceResponse priceResponse = new PriceResponse("New Name", newPrice, "PLN", "http://test.com/product");

        when(productRepository.findProductsToUpdate(any(Pageable.class))).thenReturn(List.of(product));
        when(priceClient.checkPriceByUrl(product.getProductUrl())).thenReturn(Optional.of(priceResponse));

        // Act
        int count = priceUpdateService.updateOutdatedPrices();

        // Assert
        assertThat(count).isEqualTo(1);

        verify(notificationProducer).sendEmailNotification(
                eq(product.getUserEmail()),
                eq("Price Drop Alert!"),
                contains("dropped from " + oldPrice + " to " + newPrice)
        );

        ArgumentCaptor<ProductObservation> productCaptor = ArgumentCaptor.forClass(ProductObservation.class);
        verify(productRepository).save(productCaptor.capture());

        ProductObservation savedProduct = productCaptor.getValue();
        assertThat(savedProduct.getProductName()).isEqualTo("New Name");
        assertThat(savedProduct.getCurrentPrice()).isEqualTo(newPrice);
        assertThat(savedProduct.getLastCheckedAt()).isNotNull();
    }

    @Test
    void shouldUpdatePriceButNotNotify_WhenPriceIncreases() {
        // Arrange
        BigDecimal oldPrice = new BigDecimal("100.00");
        BigDecimal newPrice = new BigDecimal("120.00");

        ProductObservation product = createProduct(oldPrice);
        PriceResponse priceResponse = new PriceResponse("New Name", newPrice, "PLN", "http://test.com/product");

        when(productRepository.findProductsToUpdate(any(Pageable.class))).thenReturn(List.of(product));
        when(priceClient.checkPriceByUrl(anyString())).thenReturn(Optional.of(priceResponse));

        // Act
        priceUpdateService.updateOutdatedPrices();

        // Assert
        verify(notificationProducer, never()).sendEmailNotification(anyString(), anyString(), anyString());
        verify(productRepository).save(product);
    }

    @Test
    void shouldOnlyUpdateTimestamp_WhenClientReturnsEmpty() {
        // Arrange
        ProductObservation product = createProduct(new BigDecimal("100.00"));
        LocalDateTime timeBefore = LocalDateTime.now().minusSeconds(1);

        when(productRepository.findProductsToUpdate(any(Pageable.class))).thenReturn(List.of(product));
        when(priceClient.checkPriceByUrl(anyString())).thenReturn(Optional.empty());

        // Act
        priceUpdateService.updateOutdatedPrices();

        // Assert
        verify(notificationProducer, never()).sendEmailNotification(anyString(), anyString(), anyString());

        ArgumentCaptor<ProductObservation> productCaptor = ArgumentCaptor.forClass(ProductObservation.class);
        verify(productRepository).save(productCaptor.capture());

        assertThat(productCaptor.getValue().getLastCheckedAt()).isAfter(timeBefore);
    }

    @Test
    void shouldHandleExceptionAndSaveTimestamp_WhenClientThrowsError() {
        // Arrange
        ProductObservation product = createProduct(new BigDecimal("100.00"));

        when(productRepository.findProductsToUpdate(any(Pageable.class))).thenReturn(List.of(product));
        when(priceClient.checkPriceByUrl(anyString())).thenThrow(new RuntimeException("Connection timeout"));

        // Act
        int count = priceUpdateService.updateOutdatedPrices();

        // Assert
        assertThat(count).isEqualTo(1);

        verify(productRepository).save(product);
        verify(notificationProducer, never()).sendEmailNotification(anyString(), anyString(), anyString());
    }

    private ProductObservation createProduct(BigDecimal currentPrice) {
        ProductObservation product = new ProductObservation();
        product.setId(1L);
        product.setProductUrl("http://test.com/product");
        product.setProductName("Old Name");
        product.setCurrentPrice(currentPrice);
        product.setUserEmail("user@test.com");
        return product;
    }
}