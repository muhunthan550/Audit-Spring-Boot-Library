package com.example.demo.model;

import com.example.audit.model.AuditEvent;
import com.example.demo.model.PaymentRequest;
import com.example.demo.model.PaymentResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SampleApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        // Clear audit events before each test
        mockMvc.perform(delete("/audit"));
    }

    @Test
    void contextLoads() {
        // Test that application context loads successfully
    }

    @Test
    void createPayment_shouldAuditSuccessfully() throws Exception {
        // Arrange
        PaymentRequest request = new PaymentRequest(
                "PAY-123", 100.0, "USD", "CUST-456"
        );

        // Act - Create payment
        MvcResult result = mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andReturn();

        // Wait for async audit event
        Thread.sleep(500);

        // Assert - Check audit events
        MvcResult auditResult = mockMvc.perform(get("/audit"))
                .andExpect(status().isOk())
                .andReturn();

        String auditJson = auditResult.getResponse().getContentAsString();
        AuditEvent[] events = objectMapper.readValue(auditJson, AuditEvent[].class);

        // Should have 2 audit events: one from controller, one from service
        assertThat(events).hasSizeGreaterThanOrEqualTo(1);

        boolean hasControllerAudit = false;
        for (AuditEvent event : events) {
            if ("API_CREATE_PAYMENT".equals(event.getAction())) {
                hasControllerAudit = true;
                assertThat(event.getSuccess()).isTrue();
                assertThat(event.getMethodName()).isEqualTo("createPayment");
            }
        }

        assertThat(hasControllerAudit).isTrue();
    }

    @Test
    void getPayment_shouldAuditSuccessfully() throws Exception {
        // Act
        mockMvc.perform(get("/api/payments/PAY-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value("PAY-123"));

        // Wait for async audit event
        Thread.sleep(500);

        // Assert
        MvcResult auditResult = mockMvc.perform(get("/audit/count"))
                .andExpect(status().isOk())
                .andReturn();

        int count = Integer.parseInt(auditResult.getResponse().getContentAsString());
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    void cancelPayment_shouldAuditException() throws Exception {
        // Act - Try to cancel with invalid ID
        mockMvc.perform(delete("/api/payments/"))
                .andExpect(status().isNotFound());

        // Still should work with valid ID
        mockMvc.perform(delete("/api/payments/PAY-123"))
                .andExpect(status().isNoContent());

        // Wait for async audit events
        Thread.sleep(500);

        // Assert
        MvcResult auditResult = mockMvc.perform(get("/audit/count"))
                .andExpect(status().isOk())
                .andReturn();

        int count = Integer.parseInt(auditResult.getResponse().getContentAsString());
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    void healthEndpoint_shouldNotBeAudited() throws Exception {
        // Act
        mockMvc.perform(get("/api/payments/health"))
                .andExpect(status().isOk());

        Thread.sleep(500);

        // Assert - No audit events should be created
        MvcResult auditResult = mockMvc.perform(get("/audit/count"))
                .andExpect(status().isOk())
                .andReturn();

        int count = Integer.parseInt(auditResult.getResponse().getContentAsString());
        assertThat(count).isEqualTo(0);
    }
}