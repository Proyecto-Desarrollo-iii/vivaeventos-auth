package co.empresa.vivaeventos.auth.delivery.rest;

import co.empresa.vivaeventos.auth.config.JwtService;
import co.empresa.vivaeventos.auth.domain.exception.CredencialesInvalidasException;
import co.empresa.vivaeventos.auth.domain.exception.TwoFactorInvalidException;
import co.empresa.vivaeventos.auth.domain.exception.UsuarioNoEncontradoException;
import co.empresa.vivaeventos.auth.domain.model.dto.TwoFactorSetupResponse;
import co.empresa.vivaeventos.auth.domain.model.Usuario;
import co.empresa.vivaeventos.auth.domain.repository.ISessionRepository;
import co.empresa.vivaeventos.auth.domain.repository.IUsuarioRepository;
import co.empresa.vivaeventos.auth.domain.service.ITwoFactorService;
import co.empresa.vivaeventos.auth.config.AuditEventClient;
import co.empresa.vivaeventos.auth.config.AuditLoggingInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import co.empresa.vivaeventos.auth.config.TestSecurityConfig;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TwoFactorController.class)
@org.springframework.context.annotation.Import(TestSecurityConfig.class)
class TwoFactorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ITwoFactorService twoFactorService;

    @MockitoBean
    private IUsuarioRepository usuarioRepository;

    @MockitoBean
    private ISessionRepository sessionRepository;

    @MockitoBean
    private JwtService jwtService;

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
    void shouldSetupTwoFactorApp() throws Exception {
        when(twoFactorService.generateSetup("test@email.com"))
                .thenReturn(new TwoFactorSetupResponse("secret", "otpauth://...", "base64img"));

        mockMvc.perform(post("/api/v1/auth/2fa/setup")
                        .with(user("test@email.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"APP\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Escanea el codigo QR con tu app de autenticacion"));
    }

    @Test
    void shouldSetupTwoFactorEmail() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("test@email.com");

        when(usuarioRepository.findByEmail("test@email.com")).thenReturn(Optional.of(usuario));
        when(twoFactorService.sendEmailCode(usuario.getId(), "test@email.com")).thenReturn("123456");

        mockMvc.perform(post("/api/v1/auth/2fa/setup")
                        .with(user("test@email.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"EMAIL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Codigo enviado a tu correo electronico"));
    }

    @Test
    void shouldSendCode() throws Exception {
        when(jwtService.isTemporaryToken("temp-token")).thenReturn(true);
        when(jwtService.isTokenExpired("temp-token")).thenReturn(false);
        when(jwtService.extractUsername("temp-token")).thenReturn("test@email.com");

        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("test@email.com");

        when(usuarioRepository.findByEmail("test@email.com")).thenReturn(Optional.of(usuario));

        mockMvc.perform(post("/api/v1/auth/2fa/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tempToken\":\"temp-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Codigo enviado a tu correo"));
    }

    @Test
    void shouldReturnBadRequestWhenSendCodeWithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/2fa/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Token requerido"));
    }

    @Test
    void shouldReturnUnauthorizedWhenTempTokenExpired() throws Exception {
        when(jwtService.isTemporaryToken("expired-token")).thenReturn(true);
        when(jwtService.isTokenExpired("expired-token")).thenReturn(true);

        mockMvc.perform(post("/api/v1/auth/2fa/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tempToken\":\"expired-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Token invalido o expirado"));
    }

    @Test
    void shouldVerifyTwoFactorCode() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("test@email.com");
        usuario.setTwoFactorEnabled(false);
        usuario.setTwoFactorSecret("testSecret");
        usuario.setTwoFactorMethod("APP");

        when(usuarioRepository.findByEmail("test@email.com")).thenReturn(Optional.of(usuario));
        when(twoFactorService.verifyCode("testSecret", "123456")).thenReturn(true);

        mockMvc.perform(post("/api/v1/auth/2fa/verify")
                        .with(user("test@email.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("2FA habilitado exitosamente"));
    }

    @Test
    void shouldReturnConflictWhenTwoFactorAlreadyEnabled() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setEmail("test@email.com");
        usuario.setTwoFactorEnabled(true);

        when(usuarioRepository.findByEmail("test@email.com")).thenReturn(Optional.of(usuario));

        mockMvc.perform(post("/api/v1/auth/2fa/verify")
                        .with(user("test@email.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"123456\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("2FA ya esta habilitado"));
    }

    @Test
    void shouldVerifyTwoFactorCodeEmailMethod() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("test@email.com");
        usuario.setTwoFactorEnabled(false);
        usuario.setTwoFactorMethod("EMAIL");

        when(usuarioRepository.findByEmail("test@email.com")).thenReturn(Optional.of(usuario));
        when(twoFactorService.verifyEmailCode(usuario.getId(), "123456")).thenReturn(true);

        mockMvc.perform(post("/api/v1/auth/2fa/verify")
                        .with(user("test@email.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("2FA habilitado exitosamente"));
    }

    @Test
    void shouldReturnBadRequestWhenVerifyInvalidCode() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("test@email.com");
        usuario.setTwoFactorEnabled(false);
        usuario.setTwoFactorSecret("testSecret");
        usuario.setTwoFactorMethod("APP");

        when(usuarioRepository.findByEmail("test@email.com")).thenReturn(Optional.of(usuario));
        when(twoFactorService.verifyCode("testSecret", "000000")).thenReturn(false);

        mockMvc.perform(post("/api/v1/auth/2fa/verify")
                        .with(user("test@email.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"000000\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Codigo de verificacion invalido"));
    }

    @Test
    void shouldDisableTwoFactor() throws Exception {
        doNothing().when(twoFactorService).disableTwoFactor("test@email.com", "pass", "123456");

        mockMvc.perform(post("/api/v1/auth/2fa/disable")
                        .with(user("test@email.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"pass\",\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("2FA deshabilitado exitosamente"));
    }

    @Test
    void shouldAuthenticate2fa() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("test@email.com");
        usuario.setTwoFactorSecret("testSecret");
        usuario.setTwoFactorMethod("APP");
        usuario.setFullName("Test User");
        usuario.setRole("CLIENT");

        when(jwtService.isTemporaryToken("temp-token")).thenReturn(true);
        when(jwtService.isTokenExpired("temp-token")).thenReturn(false);
        when(jwtService.extractUsername("temp-token")).thenReturn("test@email.com");
        when(usuarioRepository.findByEmail("test@email.com")).thenReturn(Optional.of(usuario));
        when(twoFactorService.verifyCode("testSecret", "123456")).thenReturn(true);
        when(jwtService.generateToken(usuario)).thenReturn("final-jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(86400L);

        mockMvc.perform(post("/api/v1/auth/2fa/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tempToken\":\"temp-token\",\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Autenticacion 2FA exitosa"));
    }

    @Test
    void shouldReturnBadRequestWhenAuthenticateWithMissingFields() throws Exception {
        mockMvc.perform(post("/api/v1/auth/2fa/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Token y codigo requeridos"));
    }

    @Test
    void shouldReturnUnauthorizedWhenTempTokenInvalid() throws Exception {
        when(jwtService.isTemporaryToken("invalid-token")).thenReturn(false);

        mockMvc.perform(post("/api/v1/auth/2fa/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tempToken\":\"invalid-token\",\"code\":\"123456\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Token invalido"));
    }

    @Test
    void shouldChangeTwoFactorMethod() throws Exception {
        mockMvc.perform(post("/api/v1/auth/2fa/change-method")
                        .with(user("test@email.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"EMAIL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Metodo 2FA actualizado a EMAIL"));

        verify(twoFactorService).updateTwoFactorMethod("test@email.com", "EMAIL");
    }

    @Test
    void shouldReturnBadRequestWithInvalidMethod() throws Exception {
        mockMvc.perform(post("/api/v1/auth/2fa/change-method")
                        .with(user("test@email.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"SMS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Metodo invalido. Use APP o EMAIL"));
    }

    @Test
    void shouldGetTwoFactorStatus() throws Exception {
        when(twoFactorService.isTwoFactorEnabled("test@email.com")).thenReturn(true);
        when(twoFactorService.getTwoFactorMethod("test@email.com")).thenReturn("APP");

        mockMvc.perform(get("/api/v1/auth/2fa/status")
                        .with(user("test@email.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.method").value("APP"));
    }

    @Test
    void shouldReturnNotFoundWhenSendCodeUserNotFound() throws Exception {
        when(jwtService.isTemporaryToken("temp-token")).thenReturn(true);
        when(jwtService.isTokenExpired("temp-token")).thenReturn(false);
        when(jwtService.extractUsername("temp-token")).thenReturn("missing@email.com");
        when(usuarioRepository.findByEmail("missing@email.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/auth/2fa/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tempToken\":\"temp-token\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Usuario no encontrado"));
    }

    @Test
    void shouldReturnBadRequestWhenVerifyWithInvalidCode() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("test@email.com");
        usuario.setTwoFactorEnabled(false);
        usuario.setTwoFactorSecret("testSecret");
        usuario.setTwoFactorMethod("APP");

        when(usuarioRepository.findByEmail("test@email.com")).thenReturn(Optional.of(usuario));
        when(twoFactorService.verifyCode("testSecret", "123456")).thenThrow(
                new TwoFactorInvalidException());

        mockMvc.perform(post("/api/v1/auth/2fa/verify")
                        .with(user("test@email.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Codigo de verificacion invalido"));
    }

    @Test
    void shouldReturnConflictWhenVerifyWithIllegalState() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("test@email.com");
        usuario.setTwoFactorEnabled(false);
        usuario.setTwoFactorSecret("testSecret");
        usuario.setTwoFactorMethod("APP");

        when(usuarioRepository.findByEmail("test@email.com")).thenReturn(Optional.of(usuario));
        when(twoFactorService.verifyCode("testSecret", "123456")).thenThrow(
                new IllegalStateException("2FA no configurado"));

        mockMvc.perform(post("/api/v1/auth/2fa/verify")
                        .with(user("test@email.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"123456\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("2FA no configurado"));
    }

    @Test
    void shouldReturnBadRequestWhenDisableWithInvalidCode() throws Exception {
        doThrow(new TwoFactorInvalidException())
                .when(twoFactorService).disableTwoFactor("test@email.com", "pass", "000000");

        mockMvc.perform(post("/api/v1/auth/2fa/disable")
                        .with(user("test@email.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"pass\",\"code\":\"000000\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Codigo de verificacion invalido"));
    }

    @Test
    void shouldReturnUnauthorizedWhenDisableWithWrongPassword() throws Exception {
        doThrow(new CredencialesInvalidasException())
                .when(twoFactorService).disableTwoFactor("test@email.com", "wrong", "123456");

        mockMvc.perform(post("/api/v1/auth/2fa/disable")
                        .with(user("test@email.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"wrong\",\"code\":\"123456\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Contrasena incorrecta"));
    }

    @Test
    void shouldReturnBadRequestWhenAuthenticateWithInvalidCode() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("test@email.com");
        usuario.setTwoFactorSecret("testSecret");
        usuario.setTwoFactorMethod("APP");

        when(jwtService.isTemporaryToken("temp-token")).thenReturn(true);
        when(jwtService.isTokenExpired("temp-token")).thenReturn(false);
        when(jwtService.extractUsername("temp-token")).thenReturn("test@email.com");
        when(usuarioRepository.findByEmail("test@email.com")).thenReturn(Optional.of(usuario));
        when(twoFactorService.verifyCode("testSecret", "123456")).thenThrow(
                new TwoFactorInvalidException());

        mockMvc.perform(post("/api/v1/auth/2fa/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tempToken\":\"temp-token\",\"code\":\"123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Codigo de verificacion invalido"));
    }

    @Test
    void shouldReturnUnauthorizedWhenAuthenticateWithExpiredTempToken() throws Exception {
        when(jwtService.isTemporaryToken("expired-token")).thenReturn(true);
        when(jwtService.isTokenExpired("expired-token")).thenReturn(true);

        mockMvc.perform(post("/api/v1/auth/2fa/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tempToken\":\"expired-token\",\"code\":\"123456\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Token expirado, inicie sesion nuevamente"));
    }

    @Test
    void shouldSetupTwoFactorEmailWithoutBody() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("test@email.com");

        when(twoFactorService.generateSetup("test@email.com"))
                .thenReturn(new TwoFactorSetupResponse("secret", "otpauth://...", "base64img"));

        mockMvc.perform(post("/api/v1/auth/2fa/setup")
                        .with(user("test@email.com"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Escanea el codigo QR con tu app de autenticacion"));
    }
}
