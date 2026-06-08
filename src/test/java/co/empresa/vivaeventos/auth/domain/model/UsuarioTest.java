package co.empresa.vivaeventos.auth.domain.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class UsuarioTest {

    @Test
    void shouldSetAndGetFields() {
        Usuario user = new Usuario();
        UUID id = UUID.randomUUID();

        user.setId(id);
        user.setEmail("test@email.com");
        user.setPassword("hashed-password");
        user.setFullName("Test User");
        user.setRole("CLIENT");
        user.setPhonePrefix("+57");
        user.setPhone("3001234567");
        user.setDocumentType("CC");
        user.setDocumentNumber("123456789");
        user.setCountry("Colombia");
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        user.setTwoFactorSecret("secret123");
        user.setTwoFactorEnabled(true);
        user.setTwoFactorMethod("APP");
        user.setIsActive(true);

        assertEquals(id, user.getId());
        assertEquals("test@email.com", user.getEmail());
        assertEquals("hashed-password", user.getPassword());
        assertEquals("Test User", user.getFullName());
        assertEquals("CLIENT", user.getRole());
        assertEquals("+57", user.getPhonePrefix());
        assertEquals("3001234567", user.getPhone());
        assertEquals("CC", user.getDocumentType());
        assertEquals("123456789", user.getDocumentNumber());
        assertEquals("Colombia", user.getCountry());
        assertEquals(LocalDate.of(1990, 1, 1), user.getBirthDate());
        assertEquals("secret123", user.getTwoFactorSecret());
        assertTrue(user.getTwoFactorEnabled());
        assertEquals("APP", user.getTwoFactorMethod());
        assertTrue(user.getIsActive());
    }

    @Test
    void shouldSetCreatedAtAndUpdatedAtOnCreate() {
        Usuario user = new Usuario();
        assertNull(user.getCreatedAt());
        assertNull(user.getUpdatedAt());
        user.onCreate();
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    void shouldSetUpdatedAtOnUpdate() {
        Usuario user = new Usuario();
        assertNull(user.getUpdatedAt());
        user.onUpdate();
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    void shouldReturnEmailAsUsername() {
        Usuario user = new Usuario();
        user.setEmail("test@email.com");
        assertEquals("test@email.com", user.getUsername());
    }

    @Test
    void shouldReturnRoleAuthority() {
        Usuario user = new Usuario();
        user.setRole("CLIENT");
        assertEquals(1, user.getAuthorities().size());
        assertEquals("ROLE_CLIENT", user.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void shouldBeEnabledWhenIsActiveIsTrue() {
        Usuario user = new Usuario();
        user.setIsActive(true);
        assertTrue(user.isEnabled());
    }

    @Test
    void shouldNotBeEnabledWhenIsActiveIsFalse() {
        Usuario user = new Usuario();
        user.setIsActive(false);
        assertFalse(user.isEnabled());
    }

    @Test
    void shouldNotBeEnabledWhenIsActiveIsNull() {
        Usuario user = new Usuario();
        user.setIsActive(null);
        assertFalse(user.isEnabled());
    }

    @Test
    void shouldReturnAccountNonExpired() {
        Usuario user = new Usuario();
        assertTrue(user.isAccountNonExpired());
    }

    @Test
    void shouldReturnAccountNonLocked() {
        Usuario user = new Usuario();
        assertTrue(user.isAccountNonLocked());
    }

    @Test
    void shouldReturnCredentialsNonExpired() {
        Usuario user = new Usuario();
        assertTrue(user.isCredentialsNonExpired());
    }

    @Test
    void shouldDefaultTwoFactorEnabledToFalse() {
        Usuario user = new Usuario();
        assertFalse(user.getTwoFactorEnabled());
    }

    @Test
    void shouldDefaultIsActiveToTrue() {
        Usuario user = new Usuario();
        assertTrue(user.getIsActive());
    }
}
