package co.empresa.vivaeventos.auth.domain.service;

import co.empresa.vivaeventos.auth.config.JwtService;
import co.empresa.vivaeventos.auth.domain.exception.CredencialesInvalidasException;
import co.empresa.vivaeventos.auth.domain.exception.DatosInvalidosException;
import co.empresa.vivaeventos.auth.domain.exception.UsuarioExistenteException;
import co.empresa.vivaeventos.auth.domain.exception.UsuarioNoEncontradoException;
import co.empresa.vivaeventos.auth.domain.model.Dto.AuthResponse;
import co.empresa.vivaeventos.auth.domain.model.Dto.LoginRequest;
import co.empresa.vivaeventos.auth.domain.model.Dto.RegistroRequest;
import co.empresa.vivaeventos.auth.domain.model.Usuario;
import co.empresa.vivaeventos.auth.domain.repository.IPasswordResetTokenRepository;
import co.empresa.vivaeventos.auth.domain.repository.ISessionRepository;
import co.empresa.vivaeventos.auth.domain.repository.IUsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
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

    @Test
    void shouldMapOrganizadorToOrganizer() {
        RegistroRequest request = new RegistroRequest();
        request.setEmail("org@email.com");
        request.setPassword("password123");
        request.setFullName("Org User");
        request.setRole("ORGANIZADOR");

        when(usuarioRepository.existsByEmail("org@email.com")).thenReturn(false);
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Usuario result = usuarioService.save(request);

        assertEquals("ORGANIZER", result.getRole());
    }

    @Test
    void shouldMapOrganizerAsIs() {
        RegistroRequest request = new RegistroRequest();
        request.setEmail("org2@email.com");
        request.setPassword("password123");
        request.setFullName("Org User");
        request.setRole("ORGANIZER");

        when(usuarioRepository.existsByEmail("org2@email.com")).thenReturn(false);
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Usuario result = usuarioService.save(request);

        assertEquals("ORGANIZER", result.getRole());
    }

    @Test
    void shouldMapAdminAsIs() {
        RegistroRequest request = new RegistroRequest();
        request.setEmail("admin@email.com");
        request.setPassword("password123");
        request.setFullName("Admin User");
        request.setRole("ADMIN");

        when(usuarioRepository.existsByEmail("admin@email.com")).thenReturn(false);
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Usuario result = usuarioService.save(request);

        assertEquals("ADMIN", result.getRole());
    }

    @Test
    void shouldMapGerenteToAdmin() {
        RegistroRequest request = new RegistroRequest();
        request.setEmail("gerente@email.com");
        request.setPassword("password123");
        request.setFullName("Gerente User");
        request.setRole("GERENTE");

        when(usuarioRepository.existsByEmail("gerente@email.com")).thenReturn(false);
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Usuario result = usuarioService.save(request);

        assertEquals("ADMIN", result.getRole());
    }

    @Test
    void shouldMapAdministradorToAdmin() {
        RegistroRequest request = new RegistroRequest();
        request.setEmail("admin2@email.com");
        request.setPassword("password123");
        request.setFullName("Admin User");
        request.setRole("ADMINISTRADOR");

        when(usuarioRepository.existsByEmail("admin2@email.com")).thenReturn(false);
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Usuario result = usuarioService.save(request);

        assertEquals("ADMIN", result.getRole());
    }

    @Test
    void shouldMapLogisticaAsIs() {
        RegistroRequest request = new RegistroRequest();
        request.setEmail("logistica@email.com");
        request.setPassword("password123");
        request.setFullName("Logistica User");
        request.setRole("LOGISTICA");

        when(usuarioRepository.existsByEmail("logistica@email.com")).thenReturn(false);
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Usuario result = usuarioService.save(request);

        assertEquals("LOGISTICA", result.getRole());
    }

    @Test
    void shouldMapDefaultRoleToClientWhenNull() {
        RegistroRequest request = new RegistroRequest();
        request.setEmail("default@email.com");
        request.setPassword("password123");
        request.setFullName("Default User");
        request.setRole(null);

        when(usuarioRepository.existsByEmail("default@email.com")).thenReturn(false);
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Usuario result = usuarioService.save(request);

        assertEquals("CLIENT", result.getRole());
    }

    @Test
    void shouldMapDefaultRoleToClientWhenUnknown() {
        RegistroRequest request = new RegistroRequest();
        request.setEmail("unknown@email.com");
        request.setPassword("password123");
        request.setFullName("Unknown User");
        request.setRole("SUPERADMIN");

        when(usuarioRepository.existsByEmail("unknown@email.com")).thenReturn(false);
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Usuario result = usuarioService.save(request);

        assertEquals("CLIENT", result.getRole());
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
}
