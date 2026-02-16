package com.example.audit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {

    private String action;

    private Instant timestamp;

    private Long executionTimeMs;

    private String methodName;

    private String className;

    private Map<String, Object> parameters;

    private Object returnValue;

    private String exceptionMessage;

    private Boolean success;

    private String userId;

    private Map<String, String> metadata;
}