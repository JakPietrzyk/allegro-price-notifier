package com.priceprocessor.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "scraper.api")
@Getter
@Setter
public class ScraperProperties {

    private String baseUrl;
    private Paths paths;

    @Getter
    @Setter
    public static class Paths {
        private String search;
        private String direct;
    }

    public String getSearchUrl() {
        return baseUrl + paths.search;
    }

    public String getDirectUrl() {
        return baseUrl + paths.direct;
    }
}