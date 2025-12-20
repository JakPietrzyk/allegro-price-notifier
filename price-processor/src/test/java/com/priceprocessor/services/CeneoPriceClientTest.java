package com.priceprocessor.services;

import com.priceprocessor.dtos.crawler.PriceResponse;
import com.priceprocessor.dtos.crawler.ScraperSearchRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CeneoPriceClientTest {

    @Mock
    private RestTemplate restTemplate;

    private CeneoPriceClient client;

    @BeforeEach
    void setUp() {
        client = new CeneoPriceClient(
                "http://localhost:5000/",
                restTemplate
        );
    }

    @Test
    void shouldReturnPriceResponse_WhenApiCallIsSuccessful() {
        // Arrange
        String productName = "Laptop";
        ScraperSearchRequest request = new ScraperSearchRequest(productName);

        PriceResponse expectedResponse = new PriceResponse("Laptop X", 5000.0, "PLN", "url");

        when(restTemplate.postForEntity(
                eq("http://localhost:5000/search"),
                eq(request), // Ważne: ScraperSearchRequest musi mieć poprawny equals() (rekordy to mają)
                eq(PriceResponse.class)
        )).thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        // Act
        Optional<PriceResponse> result = client.checkPriceByName(productName);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().foundProductName()).isEqualTo("Laptop X");
    }

    @Test
    void shouldReturnEmpty_WhenApiCallFails() {
        // Arrange
        when(restTemplate.postForEntity(any(), any(), any()))
                .thenThrow(new RuntimeException("Connection refused"));

        // Act
        Optional<PriceResponse> result = client.checkPriceByName("Error");

        // Assert
        assertThat(result).isEmpty();
    }
}