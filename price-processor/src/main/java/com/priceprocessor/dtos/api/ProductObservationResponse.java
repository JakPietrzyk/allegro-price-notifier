package com.priceprocessor.dtos.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.priceprocessor.models.ProductObservation;

public record ProductObservationResponse(

        @JsonProperty("productName")
        String productName,

        @JsonProperty("currentPrice")
        Double currentPrice,

        @JsonProperty("productUrl")
        String productUrl,

        @JsonProperty("lastChecked")
        String lastChecked
) {
    public ProductObservationResponse(String productName, Double currentPrice, String productUrl) {
        this(productName, currentPrice, productUrl, null);
    }

    public static ProductObservationResponse mapToDto(ProductObservation entity) {
        return new ProductObservationResponse(
                entity.getProductName(),
                entity.getCurrentPrice(),
                entity.getProductUrl()
        );
    }
}