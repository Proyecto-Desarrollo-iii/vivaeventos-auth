package co.empresa.vivaeventos.auth.delivery.rest;

import co.empresa.vivaeventos.auth.config.TestSecurityConfig;
import co.empresa.vivaeventos.auth.domain.exception.DatosInvalidosException;
import co.empresa.vivaeventos.auth.domain.exception.UsuarioExistenteException;
import co.empresa.vivaeventos.auth.domain.model.dto.ActualizarPerfilRequest;
import co.empresa.vivaeventos.auth.domain.model.dto.AuthResponse;
import co.empresa.vivaeventos.auth.domain.model.dto.LoginRequest;
import co.empresa.vivaeventos.auth.domain.model.dto.RegistroRequest;
import co.empresa.vivaeventos.auth.domain.model.Usuario;
import co.empresa.vivaeventos.auth.config.AuditEventClient;
import co.empresa.vivaeventos.auth.config.AuditLoggingInterceptor;
import co.empresa.vivaeventos.auth.domain.repository.ISessionRepository;
import co.empresa.vivaeventos.auth.domain.service.IUsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthRestController.class)
@Import(TestSecurityConfig.class)
class AuthRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IUsuarioService usuarioService;

    @MockitoBean
    private ISessionRepository sessionRepository;

    @MockitoBean
    private AuditEventClient auditEventClient;

    @MockitoBean
    private AuditLoggingInterceptor auditLoggingInterceptor;

    @BeforeEach
    void setUp() {
        when(auditLoggingInterceptor.preHandle(any(HttpServletRequest.class), any(HttpServletResponse.class), any()))
                .thenReturn(true);
    }

    @Test
    void shouldPing() throws Exception {
        mockMvc.perform(get("/api/v1/auth/ping"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldLogin() throws Exception {
        AuthResponse authResponse = new AuthResponse("token", "Bearer", "1", "a@b.com", "Test", "CLIENT", null, null, null, null, null, null, false);
        when(usuarioService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"password\":\"pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Login exitoso"))
                .andExpect(jsonPath("$.user.email").value("a@b.com"));
    }

    @Test
    void shouldRegister() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setEmail("new@b.com");
        AuthResponse authResponse = new AuthResponse("token", "Bearer", "2", "new@b.com", "New", "CLIENT", null, null, null, null, null, null, false);
        when(usuarioService.save(any(RegistroRequest.class))).thenReturn(usuario);
        when(usuarioService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@b.com\",\"password\":\"pass\",\"fullName\":\"New\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value("Usuario registrado con exito"));
    }

    @Test
    void shouldReturnBadRequestWhenRegisterWithInvalidData() throws Exception {
        when(usuarioService.save(any(RegistroRequest.class)))
                .thenThrow(new DatosInvalidosException("El email es obligatorio"));

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"pass\",\"fullName\":\"New\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El email es obligatorio"));
    }

    @Test
    void shouldReturnConflictWhenRegisterWithExistingEmail() throws Exception {
        when(usuarioService.save(any(RegistroRequest.class)))
                .thenThrow(new UsuarioExistenteException("existing@b.com"));

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"existing@b.com\",\"password\":\"pass\",\"fullName\":\"New\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Ya existe una cuenta registrada con el correo existing@b.com. Intenta iniciar sesion."));
    }

    @Test
    void shouldReturnInternalErrorWhenRegisterFails() throws Exception {
        when(usuarioService.save(any(RegistroRequest.class)))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"fail@b.com\",\"password\":\"pass\",\"fullName\":\"Fail\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Error interno del servidor"));
    }

    @Test
    void shouldValidateEmail() throws Exception {
        when(usuarioService.existsByEmail("exists@b.com")).thenReturn(true);

        mockMvc.perform(get("/api/v1/auth/validar-email")
                        .param("email", "exists@b.com"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldLogout() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Sesion cerrada exitosamente"));

        verify(sessionRepository).deleteByToken("test-token");
    }

    @Test
    void shouldLogoutWithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Sesion cerrada exitosamente"));

        verify(sessionRepository, never()).deleteByToken(any());
    }

    @Test
    void shouldSolicitarResetPassword() throws Exception {
        when(usuarioService.solicitarResetPassword("user@b.com")).thenReturn("reset-token");

        mockMvc.perform(post("/api/v1/auth/solicitar-reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@b.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Si el email existe, recibiras un enlace de recuperacion"));
    }

    @Test
    void shouldReturnBadRequestWhenSolicitarResetPasswordWithoutEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/solicitar-reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email requerido"));
    }

    @Test
    void shouldRestablecerPassword() throws Exception {
        doNothing().when(usuarioService).restablecerPassword("token-123", "newPass123");

        mockMvc.perform(post("/api/v1/auth/restablecer-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token-123\",\"newPassword\":\"newPass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Contrasena restablecida exitosamente"));
    }

    @Test
    void shouldReturnBadRequestWhenRestablecerPasswordMissingFields() throws Exception {
        mockMvc.perform(post("/api/v1/auth/restablecer-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Token y nueva contrasena requeridos"));
    }

    @Test
    void shouldReturnBadRequestWhenRestablecerPasswordFails() throws Exception {
        doThrow(new co.empresa.vivaeventos.auth.domain.exception.TokenInvalidoException("Token invalido")).when(usuarioService).restablecerPassword("bad-token", "pass");

        mockMvc.perform(post("/api/v1/auth/restablecer-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"bad-token\",\"newPassword\":\"pass\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Token invalido"));
    }

    @Test
    void shouldGetMiPerfil() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("test@email.com");
        usuario.setFullName("Test User");
        usuario.setRole("CLIENT");

        when(usuarioService.findByEmail("test@email.com")).thenReturn(usuario);

        mockMvc.perform(get("/api/v1/auth/mi-perfil")
                        .with(user("test@email.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.email").value("test@email.com"));
    }

    @Test
    void shouldActualizarMiPerfil() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("test@email.com");
        usuario.setFullName("Updated Name");

        when(usuarioService.actualizarPerfil(eq("test@email.com"), any(ActualizarPerfilRequest.class)))
                .thenReturn(usuario);

        mockMvc.perform(put("/api/v1/auth/mi-perfil")
                        .with(user("test@email.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Updated Name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Perfil actualizado exitosamente"));
    }

    @Test
    void shouldCambiarPassword() throws Exception {
        doNothing().when(usuarioService).cambiarPassword(
                "test@email.com", "oldPass", "newPass", "newPass", false);

        mockMvc.perform(put("/api/v1/auth/cambiar-password")
                        .with(user("test@email.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passwordActual\":\"oldPass\",\"nuevaPassword\":\"newPass\",\"confirmarPassword\":\"newPass\",\"cerrarOtrasSesiones\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Contrasena cambiada exitosamente"));
    }

    @Test
    void shouldReturnBadRequestWhenCambiarPasswordMismatch() throws Exception {
        doThrow(new IllegalArgumentException("Las contrasenas no coinciden"))
                .when(usuarioService).cambiarPassword(
                        "test@email.com", "oldPass", "newPass", "different", false);

        mockMvc.perform(put("/api/v1/auth/cambiar-password")
                        .with(user("test@email.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passwordActual\":\"oldPass\",\"nuevaPassword\":\"newPass\",\"confirmarPassword\":\"different\",\"cerrarOtrasSesiones\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Las contrasenas no coinciden"));
    }

    @Test
    void shouldReturnUnauthorizedWhenCambiarPasswordWrongCurrent() throws Exception {
        doThrow(new co.empresa.vivaeventos.auth.domain.exception.CredencialesInvalidasException())
                .when(usuarioService).cambiarPassword(
                        "test@email.com", "wrongPass", "newPass", "newPass", false);

        mockMvc.perform(put("/api/v1/auth/cambiar-password")
                        .with(user("test@email.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passwordActual\":\"wrongPass\",\"nuevaPassword\":\"newPass\",\"confirmarPassword\":\"newPass\",\"cerrarOtrasSesiones\":false}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("La contrasena actual no es correcta"));
    }
}
