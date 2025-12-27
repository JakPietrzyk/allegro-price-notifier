package com.priceprocessor.dtos.errors;

public record ScraperErrorResponse(
        ScraperErrorCode errorCode,
        String message
) {}