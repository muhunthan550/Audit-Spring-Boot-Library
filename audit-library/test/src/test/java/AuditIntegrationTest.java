package com.example.audit.integration;

import com.example.audit.annotation.Auditable;
import com.example.audit.config.AuditAutoConfiguration;
import com.example.audit.model.AuditEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {
                AuditAutoConfiguration.class,
                AuditIntegrationTest.TestConfiguration.class,
                AuditIntegrationTest.MockAuditController.class,
                AuditIntegrationTest.TestService.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(properties = {
        "audit.enabled=true",
        "audit.service-url=http://localhost:${local.server.port}"
})
class AuditIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestService testService;

    @Autowired
    private MockAuditController mockAuditController;

    @BeforeEach
    void setUp() {
        mockAuditController.clear();
    }

    @Test
    void shouldSendAuditEventForAnnotatedMethod() throws InterruptedException {
        // Arrange
        mockAuditController.setLatch(new CountDownLatch(1));

        // Act
        String result = testService.createPayment("payment-123", 100.0);

        // Assert
        assertThat(result).isEqualTo("Payment created: payment-123");

        boolean received = mockAuditController.getLatch().await(5, TimeUnit.SECONDS);
        assertThat(received).isTrue();

        List<AuditEvent> events = mockAuditController.getReceivedEvents();
        assertThat(events).hasSize(1);

        AuditEvent event = events.get(0);
        assertThat(event.getAction()).isEqualTo("CREATE_PAYMENT");
        assertThat(event.getMethodName()).isEqualTo("createPayment");
        assertThat(event.getSuccess()).isTrue();
        assertThat(event.getExecutionTimeMs()).isNotNull();
    }

    @Test
    void shouldCaptureExceptionInAuditEvent() throws InterruptedException {
        // Arrange
        mockAuditController.setLatch(new CountDownLatch(1));

        // Act
        try {
            testService.failingMethod();
        } catch (Exception e) {
            // Expected
        }

        // Assert
        boolean received = mockAuditController.getLatch().await(5, TimeUnit.SECONDS);
        assertThat(received).isTrue();

        List<AuditEvent> events = mockAuditController.getReceivedEvents();
        assertThat(events).hasSize(1);

        AuditEvent event = events.get(0);
        assertThat(event.getAction()).isEqualTo("FAIL_ACTION");
        assertThat(event.getSuccess()).isFalse();
        assertThat(event.getExceptionMessage()).contains("Intentional failure");
    }

    @Test
    void shouldHandleMultipleAuditEvents() throws InterruptedException {
        // Arrange
        mockAuditController.setLatch(new CountDownLatch(3));

        // Act
        testService.createPayment("payment-1", 100.0);
        testService.createPayment("payment-2", 200.0);
        testService.deletePayment("payment-1");

        // Assert
        boolean received = mockAuditController.getLatch().await(5, TimeUnit.SECONDS);
        assertThat(received).isTrue();

        List<AuditEvent> events = mockAuditController.getReceivedEvents();
        assertThat(events).hasSize(3);

        assertThat(events)
                .extracting(AuditEvent::getAction)
                .containsExactlyInAnyOrder("CREATE_PAYMENT", "CREATE_PAYMENT", "DELETE_PAYMENT");
    }

    // Test configuration
    @Configuration
    @EnableAspectJAutoProxy
    static class TestConfiguration {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    // Mock audit service controller
    @RestController
    static class MockAuditController {
        private final List<AuditEvent> receivedEvents = new ArrayList<>();
        private CountDownLatch latch;

        @PostMapping("/audit")
        public ResponseEntity<Void> receiveAudit(@RequestBody AuditEvent event) {
            receivedEvents.add(event);
            if (latch != null) {
                latch.countDown();
            }
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }

        public List<AuditEvent> getReceivedEvents() {
            return new ArrayList<>(receivedEvents);
        }

        public void clear() {
            receivedEvents.clear();
        }

        public void setLatch(CountDownLatch latch) {
            this.latch = latch;
        }

        public CountDownLatch getLatch() {
            return latch;
        }
    }

    // Test service with auditable methods
    @Service
    static class TestService {

        @Auditable(action = "CREATE_PAYMENT")
        public String createPayment(String paymentId, Double amount) {
            return "Payment created: " + paymentId;
        }

        @Auditable(action = "DELETE_PAYMENT")
        public void deletePayment(String paymentId) {
            // Delete logic
        }

        @Auditable(action = "FAIL_ACTION")
        public void failingMethod() {
            throw new RuntimeException("Intentional failure");
        }
    }
}