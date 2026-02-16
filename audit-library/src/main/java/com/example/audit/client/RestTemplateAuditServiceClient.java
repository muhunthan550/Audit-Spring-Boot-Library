package com.example.audit.client;

import com.example.audit.model.AuditEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate-based implementation of AuditServiceClient.
 * <p>
 * This implementation uses Spring's RestTemplate to send audit events
 * to an external HTTP endpoint. Failures are logged but do not propagate.
 * </p>
 */
@Slf4j
public class RestTemplateAuditServiceClient implements AuditServiceClient {

    private final RestTemplate restTemplate;
    private final String auditServiceUrl;

    /**
     * Creates a new RestTemplate-based audit service client.
     *
     * @param restTemplate the RestTemplate instance to use
     * @param auditServiceUrl the base URL of the audit service (e.g., "http://localhost:8081")
     */
    public RestTemplateAuditServiceClient(RestTemplate restTemplate, String auditServiceUrl) {
        this.restTemplate = restTemplate;
        this.auditServiceUrl = auditServiceUrl;
    }

    @Override
    public void sendAuditEvent(AuditEvent auditEvent) {
        try {
            log.debug("Sending audit event: action={}, method={}",
                    auditEvent.getAction(), auditEvent.getMethodName());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<AuditEvent> request = new HttpEntity<>(auditEvent, headers);

            String url = auditServiceUrl + "/audit";
            restTemplate.postForEntity(url, request, Void.class);

            log.debug("Audit event sent successfully: action={}", auditEvent.getAction());

        } catch (Exception e) {
            // Failure resilience: log warning but don't throw exception
            log.warn("Failed to send audit event to {}: {} - {}. Application continues normally.",
                    auditServiceUrl, e.getClass().getSimpleName(), e.getMessage());
        }
    }
}