package co.empresa.vivaeventos.auth.delivery.exception;

import co.empresa.vivaeventos.auth.domain.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void shouldHandleUsuarioNoEncontrado() {
        UsuarioNoEncontradoException ex = new UsuarioNoEncontradoException("test@email.com");
        ResponseEntity<Map<String, Object>> response = handler.handleUsuarioNoEncontrado(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Usuario no encontrado: test@email.com", Objects.requireNonNull(response.getBody()).get("error"));
    }

    @Test
    void shouldHandleUsuarioExistente() {
        UsuarioExistenteException ex = new UsuarioExistenteException("test@email.com");
        ResponseEntity<Map<String, Object>> response = handler.handleUsuarioExistente(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Ya existe una cuenta registrada con el correo test@email.com. Intenta iniciar sesion.", Objects.requireNonNull(response.getBody()).get("error"));
    }

    @Test
    void shouldHandleDatosInvalidos() {
        DatosInvalidosException ex = new DatosInvalidosException("Datos invalidos");
        ResponseEntity<Map<String, Object>> response = handler.handleDatosInvalidos(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Datos invalidos", Objects.requireNonNull(response.getBody()).get("error"));
    }

    @Test
    void shouldHandleCredencialesInvalidas() {
        CredencialesInvalidasException ex = new CredencialesInvalidasException();
        ResponseEntity<Map<String, Object>> response = handler.handleCredencialesInvalidas(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Email o contrasena incorrectos", Objects.requireNonNull(response.getBody()).get("error"));
    }

    @Test
    void shouldHandleTwoFactorRequired() {
        TwoFactorRequiredException ex = new TwoFactorRequiredException("temp-token-123", "user-id-456");
        ResponseEntity<Map<String, Object>> response = handler.handleTwoFactorRequired(ex);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("twoFactorRequired"));
        assertEquals("temp-token-123", body.get("tempToken"));
        assertEquals("user-id-456", body.get("userId"));
    }

    @Test
    void shouldHandleGeneralException() {
        Exception ex = new RuntimeException("Algo salio mal");
        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Error interno del servidor", Objects.requireNonNull(response.getBody()).get("error"));
    }

    @Test
    void shouldHandleTokenInvalido() {
        TokenInvalidoException ex = new TokenInvalidoException("Token expirado");
        ResponseEntity<Map<String, Object>> response = handler.handleTokenInvalido(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Token expirado", Objects.requireNonNull(response.getBody()).get("error"));
    }

    @Test
    void shouldHandleTwoFactorInvalid() {
        TwoFactorInvalidException ex = new TwoFactorInvalidException();
        ResponseEntity<Map<String, Object>> response = handler.handleTwoFactorInvalid(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Codigo de verificacion invalido", Objects.requireNonNull(response.getBody()).get("error"));
    }

    @Test
    void shouldHandleIllegalArgument() {
        IllegalArgumentException ex = new IllegalArgumentException("Argumento invalido");
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Argumento invalido", Objects.requireNonNull(response.getBody()).get("error"));
    }
}
