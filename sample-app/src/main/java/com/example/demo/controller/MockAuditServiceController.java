package com.example.demo.controller;

import com.example.audit.model.AuditEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/audit")
@Slf4j
public class MockAuditServiceController {

    private final List<AuditEvent> auditEvents = Collections.synchronizedList(new ArrayList<>());

    @PostMapping
    public ResponseEntity<Void> receiveAuditEvent(@RequestBody AuditEvent event) {
        log.info("📝 AUDIT EVENT RECEIVED:");
        log.info("   Action: {}", event.getAction());
        log.info("   Method: {}.{}", event.getClassName(), event.getMethodName());
        log.info("   Success: {}", event.getSuccess());
        log.info("   Execution Time: {}ms", event.getExecutionTimeMs());
        log.info("   Timestamp: {}", event.getTimestamp());

        if (event.getParameters() != null) {
            log.info("   Parameters: {}", event.getParameters());
        }

        if (event.getReturnValue() != null) {
            log.info("   Return Value: {}", event.getReturnValue());
        }

        if (event.getExceptionMessage() != null) {
            log.info("   Exception: {}", event.getExceptionMessage());
        }

        log.info("   User: {}", event.getUserId());
        log.info("---------------------------------------------------");

        auditEvents.add(event);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<AuditEvent>> getAllAuditEvents() {
        return ResponseEntity.ok(new ArrayList<>(auditEvents));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearAuditEvents() {
        auditEvents.clear();
        log.info("Audit events cleared");
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    public ResponseEntity<Integer> getAuditEventCount() {
        return ResponseEntity.ok(auditEvents.size());
    }
}