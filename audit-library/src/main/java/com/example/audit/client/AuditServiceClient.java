package com.example.audit.client;

import com.example.audit.model.AuditEvent;

public interface AuditServiceClient {
    void sendAuditEvent(AuditEvent auditEvent);
}