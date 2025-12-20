package com.priceprocessor.services.queue;

public interface NotificationProducer {
    void sendEmailNotification(String to, String subject, String body);
}