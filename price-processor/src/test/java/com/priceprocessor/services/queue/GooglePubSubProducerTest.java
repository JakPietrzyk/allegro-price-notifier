package com.priceprocessor.services.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.priceprocessor.exceptions.NotificationServiceException;
import com.priceprocessor.services.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GooglePubSubProducerTest {

    @Mock
    private PubSubTemplate pubSubTemplate;

    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private GooglePubSubProducer producer;

    private static final String TOPIC_NAME = "projects/test-project/topics/test-topic";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(producer, "topicName", TOPIC_NAME);
    }

    @Test
    void shouldPublishMessageToPubSub_WhenSerializationIsSuccessful() throws Exception {
        // Arrange
        String to = "user@test.com";
        String subject = "Price Alert";
        String body = "Price dropped!";
        String expectedJson = "{\"to\":\"user@test.com\"...}";

        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn(expectedJson);

        when(pubSubTemplate.publish(TOPIC_NAME, expectedJson))
                .thenReturn(CompletableFuture.completedFuture("message-id-123"));

        // Act
        producer.sendEmailNotification(to, subject, body);

        // Assert
        verify(objectMapper).writeValueAsString(any(Map.class));
        verify(pubSubTemplate).publish(TOPIC_NAME, expectedJson);
    }

    @Test
    void shouldThrowException_WhenJsonSerializationFails() throws Exception {
        // Arrange
        String to = "user@test.com";
        String subject = "Price Alert";
        String body = "Price dropped!";

        when(objectMapper.writeValueAsString(any(Map.class)))
                .thenThrow(new JsonProcessingException("Serialization error") {});

        // Act & Assert
        assertThatThrownBy(() -> producer.sendEmailNotification(to, subject, body))
                .isInstanceOf(NotificationServiceException.class)
                .hasMessageContaining("Failed to serialize");

        verify(pubSubTemplate, never()).publish(anyString(), anyString());
    }

    @Test
    void shouldThrowException_WhenPubSubTemplateThrowsError() throws Exception {
        // Arrange
        String to = "user@test.com";
        String subject = "Price Alert";
        String body = "Price dropped!";
        String validJson = "{}";

        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn(validJson);

        when(pubSubTemplate.publish(anyString(), anyString()))
                .thenThrow(new RuntimeException("PubSub connection failed"));

        // Act & Assert
        assertThatThrownBy(() -> producer.sendEmailNotification(to, subject, body))
                .isInstanceOf(NotificationServiceException.class)
                .hasMessageContaining("Failed to publish message");

        verify(pubSubTemplate).publish(TOPIC_NAME, validJson);
    }
}