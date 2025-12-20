package com.priceprocessor.services.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Profile("cloud")
public class GooglePubSubProducer implements NotificationProducer {

    private static final Logger logger = LoggerFactory.getLogger(GooglePubSubProducer.class);
    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gcp.pubsub.topic-name}")
    private String topicName;

    public GooglePubSubProducer(PubSubTemplate pubSubTemplate, ObjectMapper objectMapper) {
        this.pubSubTemplate = pubSubTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void sendEmailNotification(String to, String subject, String body) {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("to", to);
            payload.put("subject", subject);
            payload.put("body", body);

            String jsonString = objectMapper.writeValueAsString(payload);

            pubSubTemplate.publish(topicName, jsonString);
            logger.info("Email request sent to Google Pub/Sub: {}", to);

        } catch (Exception e) {
            logger.error("Error sending to Google Pub/Sub", e);
        }
    }
}