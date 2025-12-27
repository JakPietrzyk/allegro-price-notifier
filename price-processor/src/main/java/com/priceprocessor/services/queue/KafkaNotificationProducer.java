package com.priceprocessor.services.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.priceprocessor.exceptions.NotificationServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class KafkaNotificationProducer implements NotificationProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${notification.topic.name:allegro-price-notifications}")
    private String topicName;

    @Override
    public void sendEmailNotification(String to, String subject, String body) {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("to", to);
            payload.put("subject", subject);
            payload.put("body", body);

            String jsonString = objectMapper.writeValueAsString(payload);

            CompletableFuture<?> future = kafkaTemplate.send(topicName, jsonString);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Async error sending to Kafka for user: {}", to, ex);
                } else {
                    log.debug("Message sent to Kafka topic: {}", topicName);
                }
            });

            log.info("Email request queued in Kafka for: {}", to);

        } catch (JsonProcessingException e) {
            throw new NotificationServiceException("Failed to serialize notification payload for " + to, e);
        } catch (Exception e) {
            throw new NotificationServiceException("Failed to send message to Kafka topic " + topicName, e);
        }
    }
}