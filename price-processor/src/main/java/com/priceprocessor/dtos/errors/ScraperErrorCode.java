package com.priceprocessor.dtos.errors;

public enum ScraperErrorCode {
    MISSING_PARAM,
    PRODUCT_NOT_FOUND,
    INVALID_DOMAIN,
    CONNECTION_ERROR,
    PRICE_PARSING_ERROR,
    SCRAPING_ERROR
}