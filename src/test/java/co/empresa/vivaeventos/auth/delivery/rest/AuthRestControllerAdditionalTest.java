package co.empresa.vivaeventos.auth.delivery.rest;

import co.empresa.vivaeventos.auth.config.TestSecurityConfig;
import co.empresa.vivaeventos.auth.domain.exception.CredencialesInvalidasException;
import co.empresa.vivaeventos.auth.domain.exception.UsuarioNoEncontradoException;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthRestController.class)
@Import(TestSecurityConfig.class)
class AuthRestControllerAdditionalTest {

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
    void shouldReturnOkForSolicitarResetPasswordWhenUserNotFound() throws Exception {
        when(usuarioService.solicitarResetPassword("missing@b.com"))
                .thenThrow(new UsuarioNoEncontradoException("missing@b.com"));

        mockMvc.perform(post("/api/v1/auth/solicitar-reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"missing@b.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Si el email existe, recibiras un enlace de recuperacion"));
    }

    @Test
    void shouldReturnUnauthorizedWhenRegistroLoginFails() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setEmail("new@b.com");
        when(usuarioService.save(any(RegistroRequest.class))).thenReturn(usuario);
        when(usuarioService.login(any(LoginRequest.class)))
                .thenThrow(new CredencialesInvalidasException());

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@b.com\",\"password\":\"pass\",\"fullName\":\"New\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Email o contrasena incorrectos"));
    }

    @Test
    void shouldReturnFalseWhenValidarEmailNotFound() throws Exception {
        when(usuarioService.existsByEmail("missing@b.com")).thenReturn(false);

        mockMvc.perform(get("/api/v1/auth/validar-email")
                        .param("email", "missing@b.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existe").value(false));
    }
}
