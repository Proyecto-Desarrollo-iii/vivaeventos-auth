package co.empresa.vivaeventos.auth.config;

public record AuditEventRequest(
        String serviceName,
        String userId,
        String userRole,
        String action,
        String entityType,
        String entityId,
        String newValues,
        String ipAddress
) {}
