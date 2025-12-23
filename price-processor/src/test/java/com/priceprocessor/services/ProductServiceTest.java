package com.priceprocessor.services;

import com.priceprocessor.dtos.api.*;
import com.priceprocessor.dtos.crawler.PriceResponse;
import com.priceprocessor.models.PriceHistory;
import com.priceprocessor.models.ProductObservation;
import com.priceprocessor.repositories.ProductRepository;
import com.priceprocessor.services.clients.PriceClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PriceClient priceClient;

    @InjectMocks
    private ProductService productService;

    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    private static final String CURRENT_USER_EMAIL = "test@user.com";

    @BeforeEach
    void setUp() {
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn(CURRENT_USER_EMAIL);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldGetProductDetails_WhenProductExistsAndBelongsToUser() {
        // Arrange
        Long productId = 1L;
        ProductObservation product = createProductObservation();

        when(productRepository.findByIdAndUserEmail(productId, CURRENT_USER_EMAIL))
                .thenReturn(Optional.of(product));

        // Act
        ProductDetailsResponse result = productService.getProductDetails(productId);

        // Assert
        assertThat(result.productName()).isEqualTo(product.getProductName());
        assertThat(result.priceHistory()).hasSize(1);
        assertThat(result.priceHistory().get(0).price()).isEqualTo(product.getCurrentPrice());
    }

    @Test
    void shouldThrowException_WhenProductNotFoundOrAccessDenied() {
        // Arrange
        Long productId = 999L;
        when(productRepository.findByIdAndUserEmail(productId, CURRENT_USER_EMAIL))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.getProductDetails(productId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Product not found or access denied");
    }

    @Test
    void shouldStartObservingProductByName_WhenPriceIsFound() {
        // Arrange
        String productName = "iPhone 15";
        ProductObservationByNameRequest request = new ProductObservationByNameRequest(productName);

        PriceResponse priceResponse = new PriceResponse("iPhone 15 Pro", new BigDecimal("5000"), "PLN", "http://ceneo.pl/123");

        when(priceClient.checkPriceByName(productName)).thenReturn(Optional.of(priceResponse));

        // Act
        ProductObservationResponse result = productService.startObservingProductByName(request);

        // Assert
        ArgumentCaptor<ProductObservation> captor = ArgumentCaptor.forClass(ProductObservation.class);
        verify(productRepository).save(captor.capture());

        ProductObservation savedProduct = captor.getValue();
        assertThat(savedProduct.getUserEmail()).isEqualTo(CURRENT_USER_EMAIL);
        assertThat(savedProduct.getProductName()).isEqualTo("iPhone 15 Pro");
        assertThat(savedProduct.getCurrentPrice()).isEqualTo(new BigDecimal("5000"));
        assertThat(result.productName()).isEqualTo("iPhone 15 Pro");
    }

    @Test
    void shouldThrowException_WhenStartObservingByNameAndPriceNotFound() {
        // Arrange
        String productName = "Unicorn";
        ProductObservationByNameRequest request = new ProductObservationByNameRequest(productName);

        when(priceClient.checkPriceByName(productName)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.startObservingProductByName(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Price not found");

        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldStartObservingProductByUrl_WhenPriceIsFound() {
        // Arrange
        String url = "http://ceneo.pl/abc";

        ProductObservationByUrlRequest request = new ProductObservationByUrlRequest(url);

        PriceResponse priceResponse = new PriceResponse("Laptop", new BigDecimal("3000"), "PLN", url);

        when(priceClient.checkPriceByUrl(url)).thenReturn(Optional.of(priceResponse));

        // Act
        productService.startObservingProductByUrl(request);

        // Assert
        ArgumentCaptor<ProductObservation> captor = ArgumentCaptor.forClass(ProductObservation.class);
        verify(productRepository).save(captor.capture());

        assertThat(captor.getValue().getProductUrl()).isEqualTo(url);
        assertThat(captor.getValue().getUserEmail()).isEqualTo(CURRENT_USER_EMAIL);
    }

    @Test
    void shouldReturnAllObservedProductsForCurrentUser() {
        // Arrange
        ProductObservation p1 = createProductObservation();
        ProductObservation p2 = createProductObservation();
        p2.setProductName("Other Product");

        when(productRepository.findAllByUserEmail(CURRENT_USER_EMAIL))
                .thenReturn(List.of(p1, p2));

        // Act
        List<ProductObservationResponse> results = productService.getAllObservedProducts();

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results.get(0).productName()).isEqualTo("Test Product");
    }

    @Test
    void shouldDeleteObservedProduct_WhenExistsAndBelongsToUser() {
        // Arrange
        Long productId = 1L;
        ProductObservation product = createProductObservation();

        when(productRepository.findByIdAndUserEmail(productId, CURRENT_USER_EMAIL))
                .thenReturn(Optional.of(product));

        // Act
        productService.deleteObservedProduct(productId);

        // Assert
        verify(productRepository).delete(product);
    }

    @Test
    void shouldThrowException_WhenDeletingNonExistentProduct() {
        // Arrange
        Long productId = 1L;
        when(productRepository.findByIdAndUserEmail(productId, CURRENT_USER_EMAIL))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.deleteObservedProduct(productId))
                .isInstanceOf(RuntimeException.class);

        verify(productRepository, never()).delete(any());
    }

    private ProductObservation createProductObservation() {
        ProductObservation p = new ProductObservation();
        p.setId(1L);
        p.setProductName("Test Product");
        p.setProductUrl("http://url.com");
        p.setCurrentPrice(new BigDecimal("100.00"));
        p.setUserEmail(CURRENT_USER_EMAIL);

        List<PriceHistory> history = new ArrayList<>();
        PriceHistory h = new PriceHistory();
        h.setPrice(new BigDecimal("100.00"));
        h.setCheckedAt(LocalDateTime.now());
        history.add(h);

        p.setPriceHistory(history);
        return p;
    }
}