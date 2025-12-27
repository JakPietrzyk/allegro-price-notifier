package com.priceprocessor.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.priceprocessor.dtos.api.*;
import com.priceprocessor.dtos.errors.ErrorCode;
import com.priceprocessor.exceptions.ProductNotFoundException;
import com.priceprocessor.exceptions.ProductNotFoundInStoreException;
import com.priceprocessor.exceptions.crawler.InvalidStoreUrlException;
import com.priceprocessor.services.JwtService;
import com.priceprocessor.services.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProductController.class, properties = {
        "spring.cloud.gcp.sql.enabled=false",
        "spring.cloud.gcp.core.enabled=false"
})
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void shouldReturnListOfProducts() throws Exception {
        ProductObservationResponse p1 = new ProductObservationResponse(1L, "Laptop", new BigDecimal("3000.00"), "url1");
        ProductObservationResponse p2 = new ProductObservationResponse(2L, "Phone", new BigDecimal("1000.00"), "url2");

        when(productService.getAllObservedProducts()).thenReturn(List.of(p1, p2));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].productName").value("Laptop"))
                .andExpect(jsonPath("$[1].productName").value("Phone"));
    }

    @Test
    void shouldAddProductByName() throws Exception {
        ProductObservationByNameRequest request = new ProductObservationByNameRequest("Laptop");
        ProductObservationResponse response = new ProductObservationResponse(1L, "Laptop Pro", new BigDecimal("5000"), "http://ceneo.pl/1");

        when(productService.startObservingProductByName(any(ProductObservationByNameRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/products/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("Laptop Pro"))
                .andExpect(jsonPath("$.currentPrice").value(5000));
    }

    @Test
    void shouldAddProductByUrl() throws Exception {
        ProductObservationByUrlRequest request = new ProductObservationByUrlRequest("http://ceneo.pl/123");
        ProductObservationResponse response = new ProductObservationResponse(1L, "Console", new BigDecimal("2000"), "http://ceneo.pl/123");

        when(productService.startObservingProductByUrl(any(ProductObservationByUrlRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/products/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productUrl").value("http://ceneo.pl/123"));
    }

    @Test
    void shouldGetProductDetails() throws Exception {
        Long productId = 1L;
        ProductDetailsResponse response = new ProductDetailsResponse(productId, "TV", "http://url", new BigDecimal("2000"), "test@user",List.of());

        when(productService.getProductDetails(productId)).thenReturn(response);

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("TV"));
    }

    @Test
    void shouldDeleteProduct() throws Exception {
        Long productId = 123L;

        mockMvc.perform(delete("/api/products/{id}", productId))
                .andExpect(status().isNoContent());

        verify(productService).deleteObservedProduct(productId);
    }

    @Test
    void shouldReturn404_WhenProductNotFoundById() throws Exception {
        // Arrange
        Long nonExistentId = 999L;
        when(productService.getProductDetails(nonExistentId))
                .thenThrow(new ProductNotFoundException(nonExistentId));

        // Act & Assert
        mockMvc.perform(get("/api/products/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product with ID 999 not found or access denied."));
    }

    @Test
    void shouldReturn404_WhenProductNotFoundInStore() throws Exception {
        // Arrange
        ProductObservationByNameRequest request = new ProductObservationByNameRequest("Unicorn");
        when(productService.startObservingProductByName(any()))
                .thenThrow(new ProductNotFoundInStoreException("Unicorn"));

        // Act & Assert
        mockMvc.perform(post("/api/products/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.PRODUCT_NOT_IN_STORE.name()))
                .andExpect(jsonPath("$.message").value("Could not find product in store for search term/url: Unicorn"));
    }

    @Test
    void shouldReturn400_WhenInvalidUrlProvided() throws Exception {
        // Arrange
        ProductObservationByUrlRequest request = new ProductObservationByUrlRequest("http://fake.com");

        when(productService.startObservingProductByUrl(any()))
                .thenThrow(new InvalidStoreUrlException("Invalid link"));

        // Act & Assert
        mockMvc.perform(post("/api/products/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_FAILED.name()))
                .andExpect(jsonPath("$.message").value("Invalid link"));
    }
}