package com.priceprocessor.exceptions;

public class ProductNotFoundInStoreException extends RuntimeException {
    public ProductNotFoundInStoreException(String searchTerm) {
        super("Could not find product in store for search term/url: " + searchTerm);
    }
}