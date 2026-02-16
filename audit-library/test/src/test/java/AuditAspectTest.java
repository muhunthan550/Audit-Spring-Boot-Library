package test.src.test.java;

import com.example.audit.annotation.Auditable;
import com.example.audit.aspect.AuditAspect;
import com.example.audit.client.AuditServiceClient;
import com.example.audit.model.AuditEvent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock
    private AuditServiceClient auditServiceClient;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Captor
    private ArgumentCaptor<AuditEvent> auditEventCaptor;

    private AuditAspect auditAspect;

    @BeforeEach
    void setUp() {
        auditAspect = new AuditAspect(auditServiceClient);
    }

    @Test
    void auditMethod_shouldCaptureSuccessfulExecution() throws Throwable {
        // Arrange
        String expectedResult = "success";
        Method testMethod = TestService.class.getMethod("testMethod", String.class);
        Auditable auditable = testMethod.getAnnotation(Auditable.class);

        when(joinPoint.proceed()).thenReturn(expectedResult);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(testMethod);
        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.getArgs()).thenReturn(new Object[]{"test-param"});

        // Act
        Object result = auditAspect.auditMethod(joinPoint, auditable);

        // Assert
        assertThat(result).isEqualTo(expectedResult);
        verify(auditServiceClient, times(1)).sendAuditEvent(auditEventCaptor.capture());

        AuditEvent capturedEvent = auditEventCaptor.getValue();
        assertThat(capturedEvent.getAction()).isEqualTo("TEST_ACTION");
        assertThat(capturedEvent.getMethodName()).isEqualTo("testMethod");
        assertThat(capturedEvent.getClassName()).contains("TestService");
        assertThat(capturedEvent.getSuccess()).isTrue();
        assertThat(capturedEvent.getExceptionMessage()).isNull();
        assertThat(capturedEvent.getExecutionTimeMs()).isNotNull();
        assertThat(capturedEvent.getTimestamp()).isNotNull();
    }

    @Test
    void auditMethod_shouldCaptureException() throws Throwable {
        // Arrange
        RuntimeException expectedException = new RuntimeException("Test exception");
        Method testMethod = TestService.class.getMethod("testMethod", String.class);
        Auditable auditable = testMethod.getAnnotation(Auditable.class);

        when(joinPoint.proceed()).thenThrow(expectedException);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(testMethod);
        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.getArgs()).thenReturn(new Object[]{"test-param"});

        // Act & Assert
        assertThatThrownBy(() -> auditAspect.auditMethod(joinPoint, auditable))
                .isEqualTo(expectedException);

        verify(auditServiceClient, times(1)).sendAuditEvent(auditEventCaptor.capture());

        AuditEvent capturedEvent = auditEventCaptor.getValue();
        assertThat(capturedEvent.getSuccess()).isFalse();
        assertThat(capturedEvent.getExceptionMessage()).isEqualTo("Test exception");
        assertThat(capturedEvent.getReturnValue()).isNull();
    }

    @Test
    void auditMethod_shouldCaptureParameters() throws Throwable {
        // Arrange
        Method testMethod = TestService.class.getMethod("testMethod", String.class);
        Auditable auditable = testMethod.getAnnotation(Auditable.class);

        when(joinPoint.proceed()).thenReturn("result");
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(testMethod);
        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.getArgs()).thenReturn(new Object[]{"param-value"});

        // Act
        auditAspect.auditMethod(joinPoint, auditable);

        // Assert
        verify(auditServiceClient).sendAuditEvent(auditEventCaptor.capture());
        AuditEvent capturedEvent = auditEventCaptor.getValue();

        assertThat(capturedEvent.getParameters()).isNotNull();
        assertThat(capturedEvent.getParameters()).containsKey("arg0");
    }

    @Test
    void auditMethod_shouldNotCaptureParametersWhenDisabled() throws Throwable {
        // Arrange
        Method testMethod = TestService.class.getMethod("testMethodNoParams");
        Auditable auditable = testMethod.getAnnotation(Auditable.class);

        when(joinPoint.proceed()).thenReturn("result");
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(testMethod);
        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.getArgs()).thenReturn(new Object[]{});

        // Act
        auditAspect.auditMethod(joinPoint, auditable);

        // Assert
        verify(auditServiceClient).sendAuditEvent(auditEventCaptor.capture());
        AuditEvent capturedEvent = auditEventCaptor.getValue();

        assertThat(capturedEvent.getParameters()).isNull();
    }

    @Test
    void auditMethod_shouldNotCaptureReturnValueWhenDisabled() throws Throwable {
        // Arrange
        Method testMethod = TestService.class.getMethod("testMethodNoReturn");
        Auditable auditable = testMethod.getAnnotation(Auditable.class);

        when(joinPoint.proceed()).thenReturn("result");
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(testMethod);
        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.getArgs()).thenReturn(new Object[]{});

        // Act
        auditAspect.auditMethod(joinPoint, auditable);

        // Assert
        verify(auditServiceClient).sendAuditEvent(auditEventCaptor.capture());
        AuditEvent capturedEvent = auditEventCaptor.getValue();

        assertThat(capturedEvent.getReturnValue()).isNull();
    }

    @Test
    void auditMethod_shouldHandleAuditClientFailure() throws Throwable {
        // Arrange
        Method testMethod = TestService.class.getMethod("testMethod", String.class);
        Auditable auditable = testMethod.getAnnotation(Auditable.class);

        when(joinPoint.proceed()).thenReturn("result");
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(testMethod);
        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.getArgs()).thenReturn(new Object[]{"param"});

        doThrow(new RuntimeException("Audit service down"))
                .when(auditServiceClient).sendAuditEvent(any());

        // Act - should not throw exception
        Object result = auditAspect.auditMethod(joinPoint, auditable);

        // Assert
        assertThat(result).isEqualTo("result");
    }

    // Test service class with annotated methods
    private static class TestService {

        @Auditable(action = "TEST_ACTION")
        public String testMethod(String param) {
            return "result";
        }

        @Auditable(action = "NO_PARAMS", captureParameters = false)
        public String testMethodNoParams() {
            return "result";
        }

        @Auditable(action = "NO_RETURN", captureReturnValue = false)
        public String testMethodNoReturn() {
            return "result";
        }
    }
}