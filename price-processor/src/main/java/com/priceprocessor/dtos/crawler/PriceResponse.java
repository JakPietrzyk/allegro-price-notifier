package com.priceprocessor.dtos.crawler;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PriceResponse(
        @JsonProperty("found_product_name")
        String foundProductName,
        Double price,
        String currency,
        @JsonProperty("ceneo_url")
        String ceneoUrl
) {}