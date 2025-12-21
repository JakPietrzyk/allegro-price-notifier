package com.priceprocessor.dtos.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.priceprocessor.models.ProductObservation;

import java.math.BigDecimal;

public record ProductObservationResponse(
        Long id,

        @JsonProperty("productName")
        String productName,

        @JsonProperty("currentPrice")
        BigDecimal currentPrice,

        @JsonProperty("productUrl")
        String productUrl,

        @JsonProperty("lastChecked")
        String lastChecked
) {
    public ProductObservationResponse(Long id, String productName, BigDecimal currentPrice, String productUrl) {
        this(id, productName, currentPrice, productUrl, null);
    }

    public static ProductObservationResponse mapToDto(ProductObservation entity) {
        return new ProductObservationResponse(
                entity.getId(),
                entity.getProductName(),
                entity.getCurrentPrice(),
                entity.getProductUrl()
        );
    }
}