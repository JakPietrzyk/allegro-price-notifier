package com.priceprocessor.services.clients;

import com.priceprocessor.config.ScraperProperties;
import com.priceprocessor.dtos.crawler.PriceResponse;
import com.priceprocessor.dtos.crawler.ScraperSearchRequest;
import com.priceprocessor.dtos.crawler.ScraperUrlRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CeneoPriceClientTest {

    @Mock
    private RestTemplate restTemplate;

    private CeneoPriceClient ceneoPriceClient;
    private static final String BASE_URL = "http://scraper-api";
    private static final String SEARCH_PATH = "/find_price";
    private static final String DIRECT_PATH = "/scrape_direct_url";

    @BeforeEach
    void setUp() {
        ScraperProperties properties = new ScraperProperties();
        properties.setBaseUrl(BASE_URL);

        ScraperProperties.Paths paths = new ScraperProperties.Paths();
        paths.setSearch(SEARCH_PATH);
        paths.setDirect(DIRECT_PATH);
        properties.setPaths(paths);

        ceneoPriceClient = new CeneoPriceClient(restTemplate, properties);
    }

    @Test
    void shouldReturnPriceResponse_WhenSearchingByNameAndRequestIsSuccessful() {
        // Arrange
        String productName = "Iphone 15";
        String expectedUrl = BASE_URL + SEARCH_PATH;
        PriceResponse mockResponse = new PriceResponse("Iphone 15", BigDecimal.TEN, "PLN", "url");

        when(restTemplate.postForEntity(eq(expectedUrl), any(ScraperSearchRequest.class), eq(PriceResponse.class)))
                .thenReturn(ResponseEntity.ok(mockResponse));

        // Act
        Optional<PriceResponse> result = ceneoPriceClient.checkPriceByName(productName);

        // Assert
        assertThat(result).isPresent();
        assertThat(result).contains(mockResponse);
    }

    @Test
    void shouldReturnEmpty_WhenSearchingByNameAndScraperThrowException() {
        // Arrange
        String productName = "Iphone 15";
        String expectedUrl = BASE_URL + SEARCH_PATH;

        when(restTemplate.postForEntity(eq(expectedUrl), any(ScraperSearchRequest.class), eq(PriceResponse.class)))
                .thenThrow(new RestClientException("Connection refused"));

        // Act
        Optional<PriceResponse> result = ceneoPriceClient.checkPriceByName(productName);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmpty_WhenSearchingByNameAndResponseBodyIsNull() {
        // Arrange
        String productName = "Iphone 15";
        String expectedUrl = BASE_URL + SEARCH_PATH;

        when(restTemplate.postForEntity(eq(expectedUrl), any(ScraperSearchRequest.class), eq(PriceResponse.class)))
                .thenReturn(ResponseEntity.ok(null));

        // Act
        Optional<PriceResponse> result = ceneoPriceClient.checkPriceByName(productName);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnPriceResponse_WhenSearchingByUrlAndRequestIsSuccessful() {
        // Arrange
        String productUrl = "http://ceneo.pl/123";
        String expectedUrl = BASE_URL + DIRECT_PATH;
        PriceResponse mockResponse = new PriceResponse("TV Samsung", BigDecimal.valueOf(2000), "PLN", productUrl);

        when(restTemplate.postForEntity(eq(expectedUrl), any(ScraperUrlRequest.class), eq(PriceResponse.class)))
                .thenReturn(ResponseEntity.ok(mockResponse));

        // Act
        Optional<PriceResponse> result = ceneoPriceClient.checkPriceByUrl(productUrl);

        // Assert
        assertThat(result).isPresent();
        assertThat(result).contains(mockResponse);
    }

    @Test
    void shouldReturnEmpty_WhenSearchingByUrlAndScraperThrowsException() {
        // Arrange
        String productUrl = "http://ceneo.pl/123";
        String expectedUrl = BASE_URL + DIRECT_PATH;

        when(restTemplate.postForEntity(eq(expectedUrl), any(ScraperUrlRequest.class), eq(PriceResponse.class)))
                .thenThrow(new RestClientException("Timeout"));

        // Act
        Optional<PriceResponse> result = ceneoPriceClient.checkPriceByUrl(productUrl);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmpty_WhenSearchingByUrlAndResponseIsNotSuccessful() {
        // Arrange
        String productUrl = "http://ceneo.pl/123";
        String expectedUrl = BASE_URL + DIRECT_PATH;

        when(restTemplate.postForEntity(eq(expectedUrl), any(ScraperUrlRequest.class), eq(PriceResponse.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        // Act
        Optional<PriceResponse> result = ceneoPriceClient.checkPriceByUrl(productUrl);

        // Assert
        assertThat(result).isEmpty();
    }
}