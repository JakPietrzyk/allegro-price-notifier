package com.priceprocessor.services.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaNotificationProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private KafkaNotificationProducer producer;

    private static final String TOPIC_NAME = "test-price-notifications";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(producer, "topicName", TOPIC_NAME);
    }

    @Test
    void shouldSendNotificationToKafka_WhenSerializationIsSuccessful() throws Exception {
        // Arrange
        String to = "user@example.com";
        String subject = "Price Drop Alert!";
        String body = "Price dropped";
        String expectedJson = "{\"to\":\"user@example.com\",\"subject\":\"Price Drop Alert!\"...}";

        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn(expectedJson);

        when(kafkaTemplate.send(anyString(), anyString())).thenReturn(new CompletableFuture<>());

        // Act
        producer.sendEmailNotification(to, subject, body);

        // Assert
        verify(objectMapper).writeValueAsString(any(Map.class));
        verify(kafkaTemplate).send(TOPIC_NAME, expectedJson);
    }

    @Test
    void shouldNotSendToKafka_WhenSerializationFails() throws Exception {
        // Arrange
        String to = "user@example.com";
        String subject = "Test";
        String body = "Body";

        when(objectMapper.writeValueAsString(any(Map.class)))
                .thenThrow(new JsonProcessingException("Error mapping") {});

        // Act
        producer.sendEmailNotification(to, subject, body);

        // Assert
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    void shouldHandleException_WhenKafkaThrowsError() throws Exception {
        // Arrange
        String to = "user@example.com";
        String validJson = "{}";

        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn(validJson);

        when(kafkaTemplate.send(anyString(), anyString()))
                .thenThrow(new RuntimeException("Kafka connection error"));

        // Act
        producer.sendEmailNotification(to, "Sub", "Body");

        // Assert
        verify(kafkaTemplate).send(TOPIC_NAME, validJson);
    }
}