package com.example.audit.config;

import com.example.audit.aspect.AuditAspect;
import com.example.audit.client.AuditServiceClient;
import com.example.audit.client.RestTemplateAuditServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@AutoConfiguration
@EnableAspectJAutoProxy
@EnableConfigurationProperties(AuditProperties.class)
@ConditionalOnProperty(prefix = "audit", name = "enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class AuditAutoConfiguration {

    public AuditAutoConfiguration() {
        log.info("Audit Library Auto-Configuration activated");
    }
    @Bean(name = "auditRestTemplate")
    @ConditionalOnMissingBean(name = "auditRestTemplate")
    public RestTemplate auditRestTemplate(AuditProperties properties, RestTemplateBuilder builder) {
        log.debug("Creating RestTemplate for audit service with URL: {}", properties.getServiceUrl());

        return builder
                .setConnectTimeout(Duration.ofMillis(properties.getConnectionTimeout()))
                .setReadTimeout(Duration.ofMillis(properties.getReadTimeout()))
                .build();
    }
    @Bean
    @ConditionalOnMissingBean
    public AuditServiceClient auditServiceClient(
            RestTemplate auditRestTemplate,
            AuditProperties properties) {

        log.info("Configuring Audit Service Client with service URL: {}",
                properties.getServiceUrl());

        return new RestTemplateAuditServiceClient(auditRestTemplate, properties.getServiceUrl());
    }
    @Bean
    @ConditionalOnMissingBean
    public AuditAspect auditAspect(AuditServiceClient auditServiceClient) {
        log.info("Configuring Audit Aspect for @Auditable method interception");
        return new AuditAspect(auditServiceClient);
    }
}