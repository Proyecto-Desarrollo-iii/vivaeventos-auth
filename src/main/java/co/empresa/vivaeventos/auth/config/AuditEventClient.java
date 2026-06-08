package co.empresa.vivaeventos.auth.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class AuditEventClient {

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

    public void logEvent(String serviceName, String userId, String userRole,
                         String action, String entityType, String entityId,
                         String newValues, String ipAddress) {
        try {
            String token = Jwts.builder()
                    .subject("audit-client")
                    .claim("role", "SYSTEM")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 60000))
                    .signWith(secretKey)
                    .compact();

            Map<String, Object> body = new java.util.HashMap<>();
            body.put("serviceName", serviceName);
            body.put("userId", userId != null ? UUID.fromString(userId) : null);
            body.put("userRole", userRole);
            body.put("action", action);
            body.put("entityType", entityType);
            body.put("entityId", entityId != null ? UUID.fromString(entityId) : null);
            body.put("newValues", newValues);
            body.put("ipAddress", ipAddress);

            restTemplate.postForEntity(
                    auditServiceUrl + "/api/v1/audit/log",
                    new org.springframework.http.HttpEntity<>(body, createHeaders(token)),
                    Void.class
            );
        } catch (Exception e) {
            System.err.println("Error enviando evento de auditoria: " + e.getMessage());
        }
    }

    private org.springframework.http.HttpHeaders createHeaders(String token) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Content-Type", "application/json");
        return headers;
    }
}
