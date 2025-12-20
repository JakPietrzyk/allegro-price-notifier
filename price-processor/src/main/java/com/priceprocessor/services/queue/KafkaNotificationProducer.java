package com.priceprocessor.services.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Profile("dev")
public class KafkaNotificationProducer implements NotificationProducer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaNotificationProducer.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${notification.topic.name:allegro-price-notifications}")
    private String topicName;

    public KafkaNotificationProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
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

            kafkaTemplate.send(topicName, jsonString);
            logger.info("Email request sent to Kafka (Docker): {}", to);

        } catch (Exception e) {
            logger.error("Error sending to Kafka", e);
        }
    }
}