package com.priceprocessor.dtos.crawler;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record PriceResponse(
        @JsonProperty("found_product_name")
        String foundProductName,
        BigDecimal price,
        String currency,
        @JsonProperty("ceneo_url")
        String ceneoUrl
) {}