package com.priceprocessor.services.clients;

import com.priceprocessor.config.ScraperProperties;
import com.priceprocessor.dtos.crawler.PriceResponse;
import com.priceprocessor.dtos.crawler.ScraperSearchRequest;
import com.priceprocessor.dtos.crawler.ScraperUrlRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CeneoPriceClient implements PriceClient {

    private final RestTemplate restTemplate;
    private final ScraperProperties scraperProperties;

    @Override
    public Optional<PriceResponse> checkPriceByName(String productName) {
        return postToScraper(scraperProperties.getSearchUrl(), new ScraperSearchRequest(productName));
    }

    @Override
    public Optional<PriceResponse> checkPriceByUrl(String productUrl) {
        return postToScraper(scraperProperties.getDirectUrl(), new ScraperUrlRequest(productUrl));
    }

    private Optional<PriceResponse> postToScraper(String url, Object body) {
        try {
            ResponseEntity<PriceResponse> response = restTemplate.postForEntity(url, body, PriceResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.of(response.getBody());
            }
        } catch (Exception e) {
            log.error("Error connecting to scraper ({}): {}", url, e.getMessage());
        }
        return Optional.empty();
    }
}