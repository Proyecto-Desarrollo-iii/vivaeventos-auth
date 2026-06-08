package co.empresa.vivaeventos.auth.config;

import co.empresa.vivaeventos.auth.domain.model.Usuario;
import co.empresa.vivaeventos.auth.domain.repository.IUsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private IUsuarioRepository usuarioRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void shouldLoadUserByUsername() {
        Usuario usuario = new Usuario();
        usuario.setEmail("test@email.com");
        usuario.setPassword("encoded");
        usuario.setRole("CLIENT");

        when(usuarioRepository.findByEmail("test@email.com")).thenReturn(Optional.of(usuario));

        UserDetails result = userDetailsService.loadUserByUsername("test@email.com");

        assertNotNull(result);
        assertEquals("test@email.com", result.getUsername());
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(usuarioRepository.findByEmail("unknown@email.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("unknown@email.com"));
    }
}
