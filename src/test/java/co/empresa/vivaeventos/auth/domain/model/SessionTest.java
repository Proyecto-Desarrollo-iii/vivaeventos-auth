package co.empresa.vivaeventos.auth.domain.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class SessionTest {

    @Test
    void shouldSetAndGetFields() {
        Session session = new Session();
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        session.setId(id);
        session.setUserId(userId);
        session.setToken("test-token");
        session.setRefreshToken("refresh-token");
        session.setDeviceInfo("Mozilla/5.0");
        session.setIpAddress("192.168.1.1");
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);
        session.setExpiresAt(expiresAt);

        assertEquals(id, session.getId());
        assertEquals(userId, session.getUserId());
        assertEquals("test-token", session.getToken());
        assertEquals("refresh-token", session.getRefreshToken());
        assertEquals("Mozilla/5.0", session.getDeviceInfo());
        assertEquals("192.168.1.1", session.getIpAddress());
        assertEquals(expiresAt, session.getExpiresAt());
    }

    @Test
    void shouldSetCreatedAtOnPrePersist() {
        Session session = new Session();
        assertNull(session.getCreatedAt());
        session.onCreate();
        assertNotNull(session.getCreatedAt());
    }
}
