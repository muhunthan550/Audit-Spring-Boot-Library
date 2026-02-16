package com.example.audit.aspect;

import com.example.audit.annotation.Auditable;
import com.example.audit.client.AuditServiceClient;
import com.example.audit.model.AuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditServiceClient auditServiceClient;

    @Around("@annotation(auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        Instant startTime = Instant.now();
        Object result = null;
        Throwable exception = null;

        try {
            result = joinPoint.proceed();
            return result;

        } catch (Throwable e) {
            exception = e;
            throw e;

        } finally {

            long executionTime = System.currentTimeMillis() - startTime.toEpochMilli();

            try {
                AuditEvent auditEvent = buildAuditEvent(
                        joinPoint, auditable, startTime, executionTime, result, exception
                );
                auditServiceClient.sendAuditEvent(auditEvent);
            } catch (Exception e) {
                log.warn("Failed to build or send audit event: {}", e.getMessage());
            }
        }
    }
    private AuditEvent buildAuditEvent(
            ProceedingJoinPoint joinPoint,
            Auditable auditable,
            Instant startTime,
            long executionTime,
            Object result,
            Throwable exception) {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        return AuditEvent.builder()
                .action(auditable.action())
                .timestamp(startTime)
                .executionTimeMs(executionTime)
                .methodName(method.getName())
                .className(joinPoint.getTarget().getClass().getName())
                .parameters(auditable.captureParameters() ?
                        captureParameters(method, joinPoint.getArgs()) : null)
                .returnValue(auditable.captureReturnValue() && result != null ?
                        serializeValue(result) : null)
                .exceptionMessage(exception != null ? exception.getMessage() : null)
                .success(exception == null)
                .userId(getCurrentUserId())
                .build();
    }
    private Map<String, Object> captureParameters(Method method, Object[] args) {
        Map<String, Object> parameters = new HashMap<>();
        Parameter[] params = method.getParameters();

        for (int i = 0; i < params.length && i < args.length; i++) {
            String paramName = params[i].getName();
            Object paramValue = args[i];
            parameters.put(paramName, serializeValue(paramValue));
        }

        return parameters;
    }

    private Object serializeValue(Object value) {
        if (value == null) {
            return null;
        }

        // For primitive types and strings, return as-is
        if (value.getClass().isPrimitive() ||
                value instanceof String ||
                value instanceof Number ||
                value instanceof Boolean) {
            return value;
        }

        // For complex objects, return toString or class name
        try {
            return value.toString();
        } catch (Exception e) {
            return value.getClass().getSimpleName();
        }
    }
    private String getCurrentUserId() {

        try {
            return "system";
        } catch (Exception e) {
            return "unknown";
        }
    }
}