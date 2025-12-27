package com.priceprocessor.services.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.priceprocessor.exceptions.NotificationServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Profile("cloud")
@RequiredArgsConstructor
@Slf4j
public class GooglePubSubProducer implements NotificationProducer {

    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gcp.pubsub.topic-name}")
    private String topicName;

    @Override
    public void sendEmailNotification(String to, String subject, String body) {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("to", to);
            payload.put("subject", subject);
            payload.put("body", body);

            String jsonString = objectMapper.writeValueAsString(payload);

            CompletableFuture<String> future = pubSubTemplate.publish(topicName, jsonString);

            future.whenComplete((msgId, ex) -> {
                if (ex != null) {
                    log.error("Async error publishing to Pub/Sub for user: {}", to, ex);
                } else {
                    log.debug("Message published to Pub/Sub with ID: {}", msgId);
                }
            });

            log.info("Email notification queued for: {}", to);

        } catch (JsonProcessingException e) {
            throw new NotificationServiceException("Failed to serialize notification payload for " + to, e);
        } catch (Exception e) {
            throw new NotificationServiceException("Failed to publish message to Pub/Sub topic " + topicName, e);
        }
    }
}