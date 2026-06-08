package co.empresa.vivaeventos.auth.domain.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class TwoFactorCodeTest {

    @Test
    void shouldSetAndGetFields() {
        TwoFactorCode code = new TwoFactorCode();
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

        code.setId(id);
        code.setUserId(userId);
        code.setCode("123456");
        code.setExpiresAt(expiresAt);
        code.setUsed(false);

        assertEquals(id, code.getId());
        assertEquals(userId, code.getUserId());
        assertEquals("123456", code.getCode());
        assertEquals(expiresAt, code.getExpiresAt());
        assertFalse(code.getUsed());
    }

    @Test
    void shouldSetCreatedAtOnPrePersist() {
        TwoFactorCode code = new TwoFactorCode();
        assertNull(code.getCreatedAt());
        code.onCreate();
        assertNotNull(code.getCreatedAt());
    }

    @Test
    void shouldDefaultUsedToFalse() {
        TwoFactorCode code = new TwoFactorCode();
        assertFalse(code.getUsed());
    }
}
