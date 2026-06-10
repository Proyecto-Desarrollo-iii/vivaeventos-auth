package co.empresa.vivaeventos.auth.config;

import co.empresa.vivaeventos.auth.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "dGhpc0lzQVZlcnlTZWNyZXRLZXlGb3JWYWlhRXZlbnRvc1RoYXROZWVkczUw");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L);
        ReflectionTestUtils.setField(jwtService, "temporaryExpiration", 300000L);

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

    @Test
    void shouldExtractUsername() {
        String token = jwtService.generateToken(usuario);
        String username = jwtService.extractUsername(token);
        assertEquals("test@email.com", username);
    }

    @Test
    void shouldExtractClaim() {
        String token = jwtService.generateToken(usuario);
        String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
        assertEquals("CLIENT", role);
    }

    @Test
    void shouldDetectExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);
        String token = jwtService.generateToken(usuario);
        assertTrue(jwtService.isTokenExpired(token));
    }

    @Test
    void shouldDetectNonExpiredToken() {
        String token = jwtService.generateToken(usuario);
        assertFalse(jwtService.isTokenExpired(token));
    }

    @Test
    void shouldRejectTokenForDifferentUser() {
        String token = jwtService.generateToken(usuario);
        Usuario otroUsuario = new Usuario();
        otroUsuario.setEmail("other@email.com");
        otroUsuario.setPassword("otherPass");
        otroUsuario.setRole("ADMIN");
        otroUsuario.setIsActive(true);
        assertFalse(jwtService.isTokenValid(token, otroUsuario));
    }

    @Test
    void shouldGenerateTokenWithExtraClaimsAndRole() {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("customClaim", "customValue");
        String token = jwtService.generateToken(extraClaims, usuario);
        assertNotNull(token);
        String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
        assertEquals("CLIENT", role);
        String customValue = jwtService.extractClaim(token, claims -> claims.get("customClaim", String.class));
        assertEquals("customValue", customValue);
    }

    @Test
    void shouldGenerateTemporaryToken() {
        String token = jwtService.generateTemporaryToken(usuario);
        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(jwtService.isTemporaryToken(token));
    }

    @Test
    void shouldReturnFalseWhenTemporaryTokenIsInvalid() {
        assertFalse(jwtService.isTemporaryToken("invalid-jwt-token"));
    }

    @Test
    void shouldReturnNullExtractRoleWhenUserDetailsIsNotUsuario() {
        User springUser = new User("spring@user.com", "pass", java.util.Collections.emptyList());
        String token = jwtService.generateToken(springUser);
        assertNotNull(token);
        String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
        assertNull(role);
    }
}
