package co.empresa.vivaeventos.auth.domain.service;

import co.empresa.vivaeventos.auth.config.JwtService;
import co.empresa.vivaeventos.auth.domain.exception.CredencialesInvalidasException;
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
    void shouldMapRolesCorrectly() {
        RegistroRequest request = new RegistroRequest();
        request.setEmail("role@email.com");
        request.setPassword("pass");
        request.setRole("ORGANIZADOR");

        when(usuarioRepository.existsByEmail("role@email.com")).thenReturn(false);
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Usuario result = usuarioService.save(request);

        assertEquals("ORGANIZER", result.getRole());
    }
}
