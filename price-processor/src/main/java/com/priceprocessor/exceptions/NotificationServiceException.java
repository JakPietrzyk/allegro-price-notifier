package com.priceprocessor.exceptions;

public class NotificationServiceException extends RuntimeException {
    public NotificationServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}