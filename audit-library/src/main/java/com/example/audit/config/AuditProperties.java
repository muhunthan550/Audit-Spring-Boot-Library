package com.example.audit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "audit")
public class AuditProperties {

    private boolean enabled = true;

    private String serviceUrl = "http://localhost:8081";

    private int connectionTimeout = 5000;
    private int readTimeout = 5000;

    private boolean logEvents = true;
}