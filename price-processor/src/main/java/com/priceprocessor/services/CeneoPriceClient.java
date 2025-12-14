package com.priceprocessor.services;

import com.priceprocessor.dtos.crawler.PriceResponse;
import com.priceprocessor.dtos.crawler.ScraperSearchRequest;
import com.priceprocessor.dtos.crawler.ScraperUrlRequest;
import com.priceprocessor.services.interfaces.PriceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
@Slf4j
public class CeneoPriceClient implements PriceClient {

    private final String scraperSearchUrl;
    private final String scraperDirectUrl;
    private final RestTemplate restTemplate;

    // Spring automatycznie wstrzyknie wartości z properties ORAZ bean RestTemplate
    public CeneoPriceClient(
            @Value("${scraper.url.search}") String scraperSearchUrl,
            @Value("${scraper.url.direct}") String scraperDirectUrl,
            RestTemplate restTemplate
    ) {
        this.scraperSearchUrl = scraperSearchUrl;
        this.scraperDirectUrl = scraperDirectUrl;
        this.restTemplate = restTemplate;
    }

    @Override
    public Optional<PriceResponse> checkPriceByName(String productName) {
        return postToScraper(scraperSearchUrl, new ScraperSearchRequest(productName));
    }

    @Override
    public Optional<PriceResponse> checkPriceByUrl(String productUrl) {
        return postToScraper(scraperDirectUrl, new ScraperUrlRequest(productUrl));
    }

    private Optional<PriceResponse> postToScraper(String url, Object body) {
        try {
            ResponseEntity<PriceResponse> response = restTemplate.postForEntity(url, body, PriceResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.of(response.getBody());
            }
        } catch (Exception e) {
            log.error("Błąd połączenia ze scraperem ({}): {}", url, e.getMessage());
        }
        return Optional.empty();
    }
}