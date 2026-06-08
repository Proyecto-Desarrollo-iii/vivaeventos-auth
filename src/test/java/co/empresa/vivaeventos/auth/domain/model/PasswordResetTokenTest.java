package co.empresa.vivaeventos.auth.domain.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PasswordResetTokenTest {

    @Test
    void shouldSetAndGetFields() {
        PasswordResetToken token = new PasswordResetToken();
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        token.setId(id);
        token.setUserId(userId);
        token.setToken("reset-token-123");
        token.setExpiresAt(expiresAt);
        token.setUsedAt(null);

        assertEquals(id, token.getId());
        assertEquals(userId, token.getUserId());
        assertEquals("reset-token-123", token.getToken());
        assertEquals(expiresAt, token.getExpiresAt());
        assertNull(token.getUsedAt());
    }

    @Test
    void shouldSetCreatedAtOnPrePersist() {
        PasswordResetToken token = new PasswordResetToken();
        assertNull(token.getCreatedAt());
        token.onCreate();
        assertNotNull(token.getCreatedAt());
    }

    @Test
    void shouldBeExpiredWhenExpiresAtInPast() {
        PasswordResetToken token = new PasswordResetToken();
        token.setExpiresAt(LocalDateTime.now().minusHours(1));
        assertTrue(token.isExpired());
    }

    @Test
    void shouldNotBeExpiredWhenExpiresAtInFuture() {
        PasswordResetToken token = new PasswordResetToken();
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        assertFalse(token.isExpired());
    }

    @Test
    void shouldBeUsedWhenUsedAtNotNull() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUsedAt(LocalDateTime.now());
        assertTrue(token.isUsed());
    }

    @Test
    void shouldNotBeUsedWhenUsedAtNull() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUsedAt(null);
        assertFalse(token.isUsed());
    }
}
