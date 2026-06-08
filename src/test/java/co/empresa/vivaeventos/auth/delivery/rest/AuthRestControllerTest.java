package co.empresa.vivaeventos.auth.delivery.rest;

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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthRestController.class,
        excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
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
    void shouldValidateEmail() throws Exception {
        when(usuarioService.existsByEmail("exists@b.com")).thenReturn(true);

        mockMvc.perform(get("/api/v1/auth/validar-email")
                        .param("email", "exists@b.com"))
                .andExpect(status().isOk());
    }
}
