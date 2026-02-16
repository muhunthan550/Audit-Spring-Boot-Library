package com.example.audit.client;

import com.example.audit.model.AuditEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestTemplateAuditServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Captor
    private ArgumentCaptor<HttpEntity<AuditEvent>> requestCaptor;

    private RestTemplateAuditServiceClient client;

    private static final String SERVICE_URL = "http://localhost:8081";

    @BeforeEach
    void setUp() {
        client = new RestTemplateAuditServiceClient(restTemplate, SERVICE_URL);
    }

    @Test
    void sendAuditEvent_shouldPostToCorrectEndpoint() {
        // Arrange
        AuditEvent event = createTestAuditEvent();
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        // Act
        client.sendAuditEvent(event);

        // Assert
        verify(restTemplate).postForEntity(
                eq(SERVICE_URL + "/audit"),
                any(HttpEntity.class),
                eq(Void.class)
        );
    }

    @Test
    void sendAuditEvent_shouldSendCorrectPayload() {
        // Arrange
        AuditEvent event = createTestAuditEvent();
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        // Act
        client.sendAuditEvent(event);

        // Assert
        verify(restTemplate).postForEntity(
                anyString(),
                requestCaptor.capture(),
                eq(Void.class)
        );

        HttpEntity<AuditEvent> capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getBody()).isEqualTo(event);
        assertThat(capturedRequest.getHeaders().getContentType().toString())
                .contains("application/json");
    }

    @Test
    void sendAuditEvent_shouldNotThrowExceptionOnFailure() {
        // Arrange
        AuditEvent event = createTestAuditEvent();
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new RestClientException("Service unavailable"));

        // Act & Assert - should not throw
        client.sendAuditEvent(event);

        verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(Void.class));
    }

    @Test
    void sendAuditEvent_shouldHandleNetworkErrors() {
        // Arrange
        AuditEvent event = createTestAuditEvent();
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        // Act & Assert - should not throw
        client.sendAuditEvent(event);

        verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(Void.class));
    }

    @Test
    void sendAuditEvent_shouldHandleNullEvent() {
        // Arrange
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        // Act & Assert - should not throw
        client.sendAuditEvent(null);
    }

    private AuditEvent createTestAuditEvent() {
        return AuditEvent.builder()
                .action("TEST_ACTION")
                .timestamp(Instant.now())
                .executionTimeMs(100L)
                .methodName("testMethod")
                .className("TestClass")
                .success(true)
                .build();
    }
}