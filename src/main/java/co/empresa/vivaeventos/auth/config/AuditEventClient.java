package co.empresa.vivaeventos.auth.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class AuditEventClient {

    private static final String AUDIT_LOG_PATH = "/api/v1/audit/log";

    private final RestTemplate restTemplate;
    private final String auditServiceUrl;
    private final SecretKey secretKey;

    public AuditEventClient(RestTemplate restTemplate,
                            @Value("${services.audit.url:http://audit:8089}") String auditServiceUrl,
                            @Value("${jwt.secret}") String jwtSecret) {
        this.restTemplate = restTemplate;
        this.auditServiceUrl = auditServiceUrl;
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public void logEvent(AuditEventRequest request) {
        try {
            String token = Jwts.builder()
                    .subject("audit-client")
                    .claim("role", "SYSTEM")
                    .issuedAt(Date.from(Instant.now()))
                    .expiration(Date.from(Instant.now().plusSeconds(60)))
                    .signWith(secretKey)
                    .compact();

            Map<String, Object> body = new java.util.HashMap<>();
            body.put("serviceName", request.serviceName());
            body.put("userId", request.userId() != null ? UUID.fromString(request.userId()) : null);
            body.put("userRole", request.userRole());
            body.put("action", request.action());
            body.put("entityType", request.entityType());
            body.put("entityId", request.entityId() != null ? UUID.fromString(request.entityId()) : null);
            body.put("newValues", request.newValues());
            body.put("ipAddress", request.ipAddress());

            restTemplate.postForEntity(
                    auditServiceUrl + AUDIT_LOG_PATH,
                    new org.springframework.http.HttpEntity<>(body, createHeaders(token)),
                    Void.class
            );
        } catch (Exception e) {
            log.error("Error enviando evento de auditoria: {}", e.getMessage());
        }
    }

    private org.springframework.http.HttpHeaders createHeaders(String token) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Content-Type", "application/json");
        return headers;
    }
}
