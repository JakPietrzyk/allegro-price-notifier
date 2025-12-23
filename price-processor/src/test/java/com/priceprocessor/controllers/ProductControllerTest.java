package com.priceprocessor.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.priceprocessor.dtos.api.*;
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
        // Arrange
        ProductObservationResponse p1 = new ProductObservationResponse(1L, "Laptop", new BigDecimal("3000.00"), "url1");
        ProductObservationResponse p2 = new ProductObservationResponse(2L, "Phone", new BigDecimal("1000.00"), "url2");

        when(productService.getAllObservedProducts()).thenReturn(List.of(p1, p2));

        // Act & Assert
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].productName").value("Laptop"))
                .andExpect(jsonPath("$[1].productName").value("Phone"));
    }

    @Test
    void shouldAddProductByName() throws Exception {
        // Arrange
        ProductObservationByNameRequest request = new ProductObservationByNameRequest("Laptop");
        ProductObservationResponse response = new ProductObservationResponse(1L, "Laptop Pro", new BigDecimal("5000"), "http://ceneo.pl/1");

        when(productService.startObservingProductByName(any(ProductObservationByNameRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/products/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("Laptop Pro"))
                .andExpect(jsonPath("$.currentPrice").value(5000));
    }

    @Test
    void shouldAddProductByUrl() throws Exception {
        // Arrange
        ProductObservationByUrlRequest request = new ProductObservationByUrlRequest("http://ceneo.pl/123");
        ProductObservationResponse response = new ProductObservationResponse(1L, "Console", new BigDecimal("2000"), "http://ceneo.pl/123");

        when(productService.startObservingProductByUrl(any(ProductObservationByUrlRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/products/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productUrl").value("http://ceneo.pl/123"));
    }

    @Test
    void shouldGetProductDetails() throws Exception {
        // Arrange
        Long productId = 1L;
        ProductDetailsResponse response = new ProductDetailsResponse(productId, "TV", "http://url", new BigDecimal("2000"), "test@user",List.of());

        when(productService.getProductDetails(productId)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("TV"));
    }

    @Test
    void shouldDeleteProduct() throws Exception {
        // Arrange
        Long productId = 123L;

        // Act & Assert
        mockMvc.perform(delete("/api/products/{id}", productId))
                .andExpect(status().isNoContent());

        verify(productService).deleteObservedProduct(productId);
    }
}