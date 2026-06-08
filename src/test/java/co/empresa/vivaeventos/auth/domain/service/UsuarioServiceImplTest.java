package co.empresa.vivaeventos.auth.domain.service;

import co.empresa.vivaeventos.auth.config.JwtService;
import co.empresa.vivaeventos.auth.domain.exception.CredencialesInvalidasException;
import co.empresa.vivaeventos.auth.domain.exception.DatosInvalidosException;
import co.empresa.vivaeventos.auth.domain.exception.UsuarioExistenteException;
import co.empresa.vivaeventos.auth.domain.exception.TokenInvalidoException;
import co.empresa.vivaeventos.auth.domain.exception.UsuarioNoEncontradoException;
import co.empresa.vivaeventos.auth.domain.model.Dto.AuthResponse;
import co.empresa.vivaeventos.auth.domain.model.Dto.LoginRequest;
import co.empresa.vivaeventos.auth.domain.model.Dto.RegistroRequest;
import co.empresa.vivaeventos.auth.domain.model.Rol;
import co.empresa.vivaeventos.auth.domain.model.Usuario;
import co.empresa.vivaeventos.auth.domain.repository.IPasswordResetTokenRepository;
import co.empresa.vivaeventos.auth.domain.repository.ISessionRepository;
import co.empresa.vivaeventos.auth.domain.repository.IUsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import co.empresa.vivaeventos.auth.domain.model.Dto.ActualizarPerfilRequest;
import co.empresa.vivaeventos.auth.domain.model.PasswordResetToken;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private IUsuarioRepository usuarioRepository;
    @Mock
    private ISessionRepository sessionRepository;
    @Mock
    private IPasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    private PasswordEncoder passwordEncoder;
    private UsuarioServiceImpl usuarioService;

    private static final String ACTIVE_EMAIL = "active@email.com";
    private static final String UPDATE_EMAIL = "update@email.com";
    private static final String RESET_EMAIL = "reset@email.com";
    private static final String CHANGE_EMAIL = "change@email.com";
    private static final String USER_EMAIL = "user@email.com";
    private static final String NEW_PASS = "newPass";

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        usuarioService = new UsuarioServiceImpl(
                usuarioRepository, sessionRepository, passwordResetTokenRepository,
                passwordEncoder, jwtService, authenticationManager
        );
    }

    @Test
    void shouldRegisterUser() {
        RegistroRequest request = new RegistroRequest();
        request.setEmail("new@email.com");
        request.setPassword("password123");
        request.setFullName("New User");
        request.setRole("CLIENT");

        when(usuarioRepository.existsByEmail("new@email.com")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        Usuario result = usuarioService.save(request);

        assertNotNull(result);
        assertEquals("new@email.com", result.getEmail());
        assertTrue(passwordEncoder.matches("password123", result.getPassword()));
        assertEquals("CLIENT", result.getRole());
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        RegistroRequest request = new RegistroRequest();
        request.setEmail("existing@email.com");
        request.setPassword("password123");
        request.setFullName("Existing User");

        when(usuarioRepository.existsByEmail("existing@email.com")).thenReturn(true);

        assertThrows(UsuarioExistenteException.class, () -> usuarioService.save(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUserNotFoundByEmail() {
        when(usuarioRepository.findByEmail("missing@email.com")).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class, () -> usuarioService.findByEmail("missing@email.com"));
    }

    @Test
    void shouldThrowWhenInactiveUserLogsIn() {
        LoginRequest request = new LoginRequest("inactive@email.com", "pass");
        Usuario usuario = new Usuario();
        usuario.setEmail("inactive@email.com");
        usuario.setPassword(passwordEncoder.encode("pass"));
        usuario.setIsActive(false);

        when(usuarioRepository.findByEmail("inactive@email.com")).thenReturn(Optional.of(usuario));

        assertThrows(CredencialesInvalidasException.class, () -> usuarioService.login(request));
    }

    @Test
    void shouldReturnUserById() {
        UUID id = UUID.randomUUID();
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail("find@email.com");

        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

        Usuario result = usuarioService.findById(id);
        assertEquals("find@email.com", result.getEmail());
    }

    @ParameterizedTest
    @CsvSource({
        "ORGANIZADOR, ORGANIZER",
        "ORGANIZER,  ORGANIZER",
        "ADMIN,      ADMIN",
        "GERENTE,    ADMIN",
        "ADMINISTRADOR, ADMIN",
        "LOGISTICA,  LOGISTICA",
        "LOGISTICS,  LOGISTICA",
        "CLIENTE,    CLIENT",
        "CLIENT,     CLIENT",
        "'',         CLIENT",
        "SUPERADMIN, CLIENT"
    })
    void shouldMapRolesCorrectly(String inputRole, String expectedRole) {
        String email = "role_" + (inputRole.isEmpty() ? "empty" : inputRole.toLowerCase()) + "@email.com";

        RegistroRequest request = new RegistroRequest();
        request.setEmail(email);
        request.setPassword("password123");
        request.setFullName("Role User");
        if (!inputRole.isEmpty()) {
            request.setRole(inputRole);
        }

        when(usuarioRepository.existsByEmail(email)).thenReturn(false);
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Usuario result = usuarioService.save(request);

        assertEquals(expectedRole, result.getRole());
    }

    @Test
    void shouldContainGerenteRoleConstant() {
        Rol gerente = Rol.valueOf("GERENTE");
        assertNotNull(gerente);
        assertEquals("GERENTE", gerente.name());
    }

    private RegistroRequest requestValido() {
        RegistroRequest request = new RegistroRequest();
        request.setEmail("valido@email.com");
        request.setPassword("password123");
        request.setFullName("Usuario Valido");
        request.setRole("CLIENT");
        return request;
    }

    @Test
    void shouldThrowWhenEmailIsBlank() {
        RegistroRequest request = requestValido();
        request.setEmail("   ");

        assertThrows(DatosInvalidosException.class, () -> usuarioService.save(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenEmailFormatIsInvalid() {
        RegistroRequest request = requestValido();
        request.setEmail("correo-sin-arroba");

        assertThrows(DatosInvalidosException.class, () -> usuarioService.save(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenPasswordIsMissing() {
        RegistroRequest request = requestValido();
        request.setPassword(null);

        assertThrows(DatosInvalidosException.class, () -> usuarioService.save(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenPasswordIsTooShort() {
        RegistroRequest request = requestValido();
        request.setPassword("123");

        assertThrows(DatosInvalidosException.class, () -> usuarioService.save(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenFullNameIsBlank() {
        RegistroRequest request = requestValido();
        request.setFullName("  ");

        assertThrows(DatosInvalidosException.class, () -> usuarioService.save(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenDocumentNumberIsNotNumeric() {
        RegistroRequest request = requestValido();
        request.setDocumentNumber("12A45");

        assertThrows(DatosInvalidosException.class, () -> usuarioService.save(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenPhoneIsNotNumeric() {
        RegistroRequest request = requestValido();
        request.setPhone("300-123-4567");

        assertThrows(DatosInvalidosException.class, () -> usuarioService.save(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenBirthDateIsInTheFuture() {
        RegistroRequest request = requestValido();
        request.setBirthDate(LocalDate.now().plusDays(1));

        assertThrows(DatosInvalidosException.class, () -> usuarioService.save(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void shouldLoginSuccessfully() {
        LoginRequest request = new LoginRequest(ACTIVE_EMAIL, "pass");
        UUID userId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        usuario.setId(userId);
        usuario.setEmail(ACTIVE_EMAIL);
        usuario.setPassword(passwordEncoder.encode("pass"));
        usuario.setFullName("Active User");
        usuario.setRole("ORGANIZER");
        usuario.setIsActive(true);

        when(usuarioRepository.findByEmail(ACTIVE_EMAIL)).thenReturn(Optional.of(usuario));
        when(jwtService.generateToken(usuario)).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(86400L);

        AuthResponse response = usuarioService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("Bearer", response.getType());
        assertEquals(userId.toString(), response.getUserId());
        assertEquals(ACTIVE_EMAIL, response.getEmail());
        assertEquals("Active User", response.getFullName());
        assertEquals("ORGANIZER", response.getRole());
        verify(sessionRepository).save(any());
    }

    @Test
    void shouldCheckIfEmailExists() {
        when(usuarioRepository.existsByEmail("exists@email.com")).thenReturn(true);
        assertTrue(usuarioService.existsByEmail("exists@email.com"));

        when(usuarioRepository.existsByEmail("new@email.com")).thenReturn(false);
        assertFalse(usuarioService.existsByEmail("new@email.com"));
    }

    @Test
    void shouldUpdateProfile() {
        UUID userId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        usuario.setId(userId);
        usuario.setEmail(UPDATE_EMAIL);
        usuario.setFullName("Old Name");

        when(usuarioRepository.findByEmail(UPDATE_EMAIL)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ActualizarPerfilRequest request = new ActualizarPerfilRequest();
        request.setFullName("New Name");
        request.setPhonePrefix("+57");
        request.setPhone("3001234567");

        Usuario result = usuarioService.actualizarPerfil(UPDATE_EMAIL, request);

        assertEquals("New Name", result.getFullName());
        assertEquals("+57", result.getPhonePrefix());
        assertEquals("3001234567", result.getPhone());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void shouldSolicitarResetPassword() {
        UUID userId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        usuario.setId(userId);
        usuario.setEmail(RESET_EMAIL);

        when(usuarioRepository.findByEmail(RESET_EMAIL)).thenReturn(Optional.of(usuario));
        when(passwordResetTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String token = usuarioService.solicitarResetPassword(RESET_EMAIL);

        assertNotNull(token);
        verify(passwordResetTokenRepository).save(any());
    }

    @Test
    void shouldChangePasswordSuccessfully() {
        String currentPass = "oldPass123";
        String newPass = "newPass456";
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail(CHANGE_EMAIL);
        usuario.setPassword(passwordEncoder.encode(currentPass));

        when(usuarioRepository.findByEmail(CHANGE_EMAIL)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        usuarioService.cambiarPassword(CHANGE_EMAIL, currentPass, newPass, newPass, false);

        verify(usuarioRepository).save(any());
    }

    @Test
    void shouldThrowWhenPasswordsDoNotMatch() {
        assertThrows(IllegalArgumentException.class, () ->
                usuarioService.cambiarPassword("email@email.com", "pass", NEW_PASS, "differentConfirm", false));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenCurrentPasswordIsIncorrect() {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail(USER_EMAIL);
        usuario.setPassword(passwordEncoder.encode("correctPass"));

        when(usuarioRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(usuario));

        assertThrows(CredencialesInvalidasException.class, () ->
                usuarioService.cambiarPassword(USER_EMAIL, "wrongPass", NEW_PASS, NEW_PASS, false));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenSaveRequestIsNull() {
        assertThrows(DatosInvalidosException.class, () -> usuarioService.save(null));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenEmailIsNull() {
        RegistroRequest request = requestValido();
        request.setEmail(null);

        assertThrows(DatosInvalidosException.class, () -> usuarioService.save(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenPasswordIsBlank() {
        RegistroRequest request = requestValido();
        request.setPassword("   ");

        assertThrows(DatosInvalidosException.class, () -> usuarioService.save(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenFullNameIsNull() {
        RegistroRequest request = requestValido();
        request.setFullName(null);

        assertThrows(DatosInvalidosException.class, () -> usuarioService.save(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void shouldRestablecerPassword() {
        UUID userId = UUID.randomUUID();
        String tokenStr = "reset-token-123";

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(tokenStr);
        resetToken.setUserId(userId);
        resetToken.setExpiresAt(LocalDateTime.now().plusHours(1));

        Usuario usuario = new Usuario();
        usuario.setId(userId);
        usuario.setEmail("user@email.com");
        usuario.setPassword(passwordEncoder.encode("oldPass"));

        when(passwordResetTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(resetToken));
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(passwordResetTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        usuarioService.restablecerPassword(tokenStr, "newPassword123");

        verify(usuarioRepository).save(usuario);
        assertTrue(passwordEncoder.matches("newPassword123", usuario.getPassword()));
        assertNotNull(resetToken.getUsedAt());
    }

    @Test
    void shouldThrowWhenResetTokenNotFound() {
        when(passwordResetTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        assertThrows(TokenInvalidoException.class,
                () -> usuarioService.restablecerPassword("invalid-token", "newPassword123"));
    }

    @Test
    void shouldThrowWhenResetTokenExpired() {
        String tokenStr = "expired-token";
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(tokenStr);
        resetToken.setUserId(UUID.randomUUID());
        resetToken.setExpiresAt(LocalDateTime.now().minusHours(1));

        when(passwordResetTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(resetToken));

        assertThrows(TokenInvalidoException.class,
                () -> usuarioService.restablecerPassword(tokenStr, "newPassword123"));
    }

    @Test
    void shouldThrowWhenResetTokenAlreadyUsed() {
        String tokenStr = "used-token";
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(tokenStr);
        resetToken.setUserId(UUID.randomUUID());
        resetToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        resetToken.setUsedAt(LocalDateTime.now());

        when(passwordResetTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(resetToken));

        assertThrows(TokenInvalidoException.class,
                () -> usuarioService.restablecerPassword(tokenStr, "newPassword123"));
    }

    @Test
    void shouldThrowTwoFactorRequiredDuringLogin() {
        LoginRequest request = new LoginRequest("2fa@email.com", "pass");
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("2fa@email.com");
        usuario.setPassword(passwordEncoder.encode("pass"));
        usuario.setIsActive(true);
        usuario.setTwoFactorEnabled(true);

        when(usuarioRepository.findByEmail("2fa@email.com")).thenReturn(Optional.of(usuario));
        when(jwtService.generateTemporaryToken(usuario)).thenReturn("temp-token");

        assertThrows(co.empresa.vivaeventos.auth.domain.exception.TwoFactorRequiredException.class,
                () -> usuarioService.login(request));
    }

    @Test
    void shouldCloseOtherSessionsOnPasswordChange() {
        UUID userId = UUID.randomUUID();
        String currentPass = "oldPass123";
        String newPass = "newPass456";
        Usuario usuario = new Usuario();
        usuario.setId(userId);
        usuario.setEmail("close@email.com");
        usuario.setPassword(passwordEncoder.encode(currentPass));

        when(usuarioRepository.findByEmail("close@email.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        usuarioService.cambiarPassword("close@email.com", currentPass, newPass, newPass, true);

        verify(usuarioRepository).save(any());
        verify(sessionRepository).deleteByUserId(userId);
    }
}
