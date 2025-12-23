package com.priceprocessor.dtos.api;

public record ProductObservationRequest(String productName, String userEmail, String productUrl) {
    public ProductObservationRequest(String productName, String userEmail) {
        this(productName, userEmail, "");
    }
}