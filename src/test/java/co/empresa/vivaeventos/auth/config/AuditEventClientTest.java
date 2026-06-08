package co.empresa.vivaeventos.auth.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditEventClientTest {

    @Mock
    private RestTemplate restTemplate;

    private AuditEventClient auditEventClient;

    @Captor
    private ArgumentCaptor<HttpEntity<Map<String, Object>>> entityCaptor;

    @BeforeEach
    void setUp() {
        String jwtSecret = "dGhpc0lzQVZlcnlTZWNyZXRLZXlGb3JWYWlhRXZlbnRvc1RoYXROZWVkczUw";
        auditEventClient = new AuditEventClient(restTemplate, "http://audit:8089", jwtSecret);
    }

    @Test
    void shouldSendAuditEvent() {
        auditEventClient.logEvent(new AuditEventRequest("auth", "550e8400-e29b-41d4-a716-446655440000", "CLIENT",
                "LOGIN", "usuario", "550e8400-e29b-41d4-a716-446655440000",
                "{\"email\":\"test@test.com\"}", "192.168.1.1"));

        verify(restTemplate).postForEntity(
                eq("http://audit:8089/api/v1/audit/log"),
                any(HttpEntity.class),
                eq(Void.class)
        );
    }

    @Test
    void shouldIncludeAuthorizationHeader() {
        auditEventClient.logEvent(new AuditEventRequest("auth", null, null,
                "HTTP_REQUEST", "GET", null, "{}", null));

        verify(restTemplate).postForEntity(
                anyString(),
                entityCaptor.capture(),
                eq(Void.class)
        );

        HttpEntity<Map<String, Object>> entity = entityCaptor.getValue();
        assertNotNull(entity.getHeaders());
        assertTrue(entity.getHeaders().get("Authorization").get(0).startsWith("Bearer "));
        assertEquals("application/json", entity.getHeaders().get("Content-Type").get(0));
    }

    @Test
    void shouldIncludeRequestBodyFields() {
        auditEventClient.logEvent(new AuditEventRequest("orders", "660e8400-e29b-41d4-a716-446655440001", "ADMIN",
                "CREATE", "order", "660e8400-e29b-41d4-a716-446655440001",
                "{\"total\":100}", "10.0.0.1"));

        verify(restTemplate).postForEntity(
                anyString(),
                entityCaptor.capture(),
                eq(Void.class)
        );

        Map<String, Object> body = entityCaptor.getValue().getBody();
        assertEquals("orders", body.get("serviceName"));
        assertEquals("660e8400-e29b-41d4-a716-446655440001", body.get("userId").toString());
        assertEquals("ADMIN", body.get("userRole"));
        assertEquals("CREATE", body.get("action"));
        assertEquals("order", body.get("entityType"));
        assertEquals("660e8400-e29b-41d4-a716-446655440001", body.get("entityId").toString());
        assertEquals("{\"total\":100}", body.get("newValues"));
        assertEquals("10.0.0.1", body.get("ipAddress"));
    }

    @Test
    void shouldHandleRestTemplateExceptionGracefully() {
        when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        assertDoesNotThrow(() ->
                auditEventClient.logEvent(new AuditEventRequest("auth", null, null,
                        "TEST", "test", null, null, null))
        );
    }
}
