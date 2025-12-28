package com.priceprocessor.services.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.priceprocessor.config.ScraperProperties;
import com.priceprocessor.dtos.crawler.PriceResponse;
import com.priceprocessor.dtos.crawler.ScraperSearchRequest;
import com.priceprocessor.dtos.crawler.ScraperUrlRequest;
import com.priceprocessor.dtos.errors.ScraperErrorCode;
import com.priceprocessor.dtos.errors.ScraperErrorResponse;
import com.priceprocessor.exceptions.PriceFetchException;
import com.priceprocessor.exceptions.ProductNotFoundInStoreException;
import com.priceprocessor.exceptions.crawler.InvalidStoreUrlException;
import com.priceprocessor.exceptions.crawler.ScraperException;
import com.priceprocessor.services.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CeneoPriceClientTest {

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private MetricsService metricsService;

    private CeneoPriceClient ceneoPriceClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
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

        ceneoPriceClient = new CeneoPriceClient(restTemplate, objectMapper, properties, metricsService);
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
    void shouldThrowException_WhenSearchingByNameAndConnectionFails() {
        String productName = "Iphone 15";
        String expectedUrl = BASE_URL + SEARCH_PATH;

        when(restTemplate.postForEntity(eq(expectedUrl), any(ScraperSearchRequest.class), eq(PriceResponse.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        // Act & Assert
        assertThatThrownBy(() -> ceneoPriceClient.checkPriceByName(productName))
                .isInstanceOf(PriceFetchException.class)
                .hasMessageContaining("network error");
    }

    @Test
    void shouldThrowProductNotFoundException_AndIncrementMetric_WhenScraperReturnsProductNotFoundCode() throws JsonProcessingException {
        // Arrange
        String productName = "Unicorn";
        String expectedUrl = BASE_URL + SEARCH_PATH;

        ScraperErrorResponse errorResponse = new ScraperErrorResponse(ScraperErrorCode.PRODUCT_NOT_FOUND, "Not found");
        String jsonError = objectMapper.writeValueAsString(errorResponse);

        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.NOT_FOUND,
                "Not Found",
                HttpHeaders.EMPTY,
                jsonError.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        when(restTemplate.postForEntity(eq(expectedUrl), any(ScraperSearchRequest.class), eq(PriceResponse.class)))
                .thenThrow(exception);

        // Act
        assertThatThrownBy(() -> ceneoPriceClient.checkPriceByName(productName))
                .isInstanceOf(ProductNotFoundInStoreException.class)
                .hasMessage("Could not find product in store for search term/url: Not found");

        verify(metricsService).incrementScraperError(ScraperErrorCode.PRODUCT_NOT_FOUND.name());
    }

    @Test
    void shouldThrowInvalidUrlException_AndIncrementMetric_WhenScraperReturnsInvalidDomainCode() throws JsonProcessingException {
        // Arrange
        String productUrl = "http://bad-url.com";
        String expectedUrl = BASE_URL + DIRECT_PATH;

        ScraperErrorResponse errorResponse = new ScraperErrorResponse(ScraperErrorCode.INVALID_DOMAIN, "Invalid domain");
        String jsonError = objectMapper.writeValueAsString(errorResponse);

        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                HttpHeaders.EMPTY,
                jsonError.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        when(restTemplate.postForEntity(eq(expectedUrl), any(ScraperUrlRequest.class), eq(PriceResponse.class)))
                .thenThrow(exception);

        // Act & Assert
        assertThatThrownBy(() -> ceneoPriceClient.checkPriceByUrl(productUrl))
                .isInstanceOf(InvalidStoreUrlException.class)
                .hasMessage("Invalid domain");

        verify(metricsService).incrementScraperError(ScraperErrorCode.INVALID_DOMAIN.name());
    }

    @Test
    void shouldThrowScraperException_AndIncrementMetric_WhenScraperReturnsParsingError() throws JsonProcessingException {
        // Arrange
        String productUrl = "http://ceneo.pl/123";
        String expectedUrl = BASE_URL + DIRECT_PATH;

        ScraperErrorResponse errorResponse = new ScraperErrorResponse(ScraperErrorCode.PRICE_PARSING_ERROR, "Parsing error");
        String jsonError = objectMapper.writeValueAsString(errorResponse);

        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Unprocessable",
                HttpHeaders.EMPTY,
                jsonError.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        when(restTemplate.postForEntity(eq(expectedUrl), any(ScraperUrlRequest.class), eq(PriceResponse.class)))
                .thenThrow(exception);

        // Act & Assert
        assertThatThrownBy(() -> ceneoPriceClient.checkPriceByUrl(productUrl))
                .isInstanceOf(ScraperException.class)
                .hasMessage("Invalid price found");

        verify(metricsService).incrementScraperError(ScraperErrorCode.PRICE_PARSING_ERROR.name());
    }

    @Test
    void shouldReturnEmpty_WhenResponseBodyIsNullButStatus200() {
        String productName = "Iphone 15";
        String expectedUrl = BASE_URL + SEARCH_PATH;

        when(restTemplate.postForEntity(eq(expectedUrl), any(ScraperSearchRequest.class), eq(PriceResponse.class)))
                .thenReturn(ResponseEntity.ok(null));

        Optional<PriceResponse> result = ceneoPriceClient.checkPriceByName(productName);

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