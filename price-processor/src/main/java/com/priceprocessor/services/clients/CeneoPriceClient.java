package com.priceprocessor.services.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.priceprocessor.config.ScraperProperties;
import com.priceprocessor.dtos.crawler.PriceResponse;
import com.priceprocessor.dtos.crawler.ScraperSearchRequest;
import com.priceprocessor.dtos.crawler.ScraperUrlRequest;
import com.priceprocessor.dtos.errors.ScraperErrorResponse;
import com.priceprocessor.exceptions.PriceFetchException;
import com.priceprocessor.exceptions.ProductNotFoundInStoreException;
import com.priceprocessor.exceptions.crawler.InvalidStoreUrlException;
import com.priceprocessor.exceptions.crawler.ScraperException;
import com.priceprocessor.services.MetricsService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CeneoPriceClient implements PriceClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ScraperProperties scraperProperties;
    private final MetricsService metricsService;

    @Override
    @Timed(value = "scraper.client.request", description = "Time taken to call python scraper", extraTags = {"type", "searchByName"})
    public Optional<PriceResponse> checkPriceByName(String productName) {
        return postToScraper(scraperProperties.getSearchUrl(), new ScraperSearchRequest(productName));
    }

    @Override
    @Timed(value = "scraper.client.request", description = "Time taken to call python scraper", extraTags = {"type", "directUrl"})
    public Optional<PriceResponse> checkPriceByUrl(String productUrl) {
        return postToScraper(scraperProperties.getDirectUrl(), new ScraperUrlRequest(productUrl));
    }

    private Optional<PriceResponse> postToScraper(String url, Object body) {
        try {
            ResponseEntity<PriceResponse> response = restTemplate.postForEntity(url, body, PriceResponse.class);

            if (response.getBody() != null) {
                return Optional.of(response.getBody());
            } else {
                log.warn("Scraper returned 200 OK but empty body for url: {}", url);
                return Optional.empty();
            }

        } catch (HttpClientErrorException e) {
            handleScraperError(e);
            return Optional.empty();

        } catch (ResourceAccessException e) {
            log.error("Network error connecting to scraper: {}", e.getMessage());
            throw new PriceFetchException("Scraper network error (timeout/unreachable)", e);

        } catch (Exception e) {
            log.error("Unknown error fetching price: {}", e.getMessage(), e);
            throw new PriceFetchException("Unexpected scraper error", e);
        }
    }

    private void handleScraperError(HttpClientErrorException e) {
        ScraperErrorResponse errorResponse;

        try {
            String responseBody = e.getResponseBodyAsString();
            errorResponse = objectMapper.readValue(responseBody, ScraperErrorResponse.class);
        } catch (JsonProcessingException jsonEx) {
            log.error("Could not parse scraper exception body. Status: {}", e.getStatusCode(), jsonEx);
            throw new ScraperException("Error during communication to scraper (Unknown format)");
        }

        log.warn("Scraper Error: {}", errorResponse.errorCode());

        metricsService.incrementScraperError(errorResponse.errorCode().name());

        switch (errorResponse.errorCode()) {
            case PRODUCT_NOT_FOUND -> throw new ProductNotFoundInStoreException(errorResponse.message());

            case INVALID_DOMAIN, MISSING_PARAM -> throw new InvalidStoreUrlException(errorResponse.message());

            case PRICE_PARSING_ERROR -> throw new ScraperException("Invalid price found");

            case CONNECTION_ERROR -> throw new ScraperException("Scraper is unreachable");

            default -> throw new ScraperException("Unknown error in scraper: " + errorResponse.message());
        }
    }
}