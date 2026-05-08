package co.empresa.vivaeventos.auth.config;

import co.empresa.vivaeventos.auth.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "dGhpc0lzQVZlcnlTZWNyZXRLZXlGb3JWYWlhRXZlbnRvc1RoYXROZWVkczUw");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L);

        usuario = new Usuario();
        usuario.setEmail("test@email.com");
        usuario.setPassword("encodedPassword");
        usuario.setRole("CLIENT");
        usuario.setIsActive(true);
    }

    @Test
    void shouldGenerateAndValidateToken() {
        String token = jwtService.generateToken(usuario);
        assertNotNull(token);
        assertFalse(token.isBlank());

        String extractedUsername = jwtService.extractUsername(token);
        assertEquals("test@email.com", extractedUsername);

        assertTrue(jwtService.isTokenValid(token, usuario));
    }

    @Test
    void shouldRejectExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);
        String token = jwtService.generateToken(usuario);

        assertFalse(jwtService.isTokenValid(token, usuario));
    }

    @Test
    void shouldReturnExpirationInSeconds() {
        long seconds = jwtService.getExpirationSeconds();
        assertEquals(86400L, seconds);
    }
}
