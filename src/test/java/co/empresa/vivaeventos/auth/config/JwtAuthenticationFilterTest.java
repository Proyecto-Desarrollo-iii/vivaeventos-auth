package co.empresa.vivaeventos.auth.config;

import co.empresa.vivaeventos.auth.domain.model.Session;
import co.empresa.vivaeventos.auth.domain.model.Usuario;
import co.empresa.vivaeventos.auth.domain.repository.ISessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private ISessionRepository sessionRepository;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService, sessionRepository);
    }

    @Test
    void shouldSkipFilterForPublicEndpoints() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldSkipFilterForPublicEndpointsPing() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/ping");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldSkipFilterWhenNoAuthHeader() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/mi-perfil");
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSkipFilterWhenAuthHeaderNotBearer() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/mi-perfil");
        when(request.getHeader("Authorization")).thenReturn("Basic token");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSkipFilterWhenSessionNotValid() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/mi-perfil");
        when(request.getHeader("Authorization")).thenReturn("Bearer jwt-token");

        when(sessionRepository.findByTokenAndExpiresAtAfter(eq("jwt-token"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).extractUsername(any());
    }

    @Test
    void shouldAuthenticateWhenValidSessionAndToken() throws ServletException, IOException {
        Usuario usuario = new Usuario();
        usuario.setEmail("test@email.com");
        usuario.setRole("CLIENT");

        when(request.getRequestURI()).thenReturn("/api/v1/auth/mi-perfil");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-jwt");

        Session session = new Session();
        session.setToken("valid-jwt");
        when(sessionRepository.findByTokenAndExpiresAtAfter(eq("valid-jwt"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(session));

        when(jwtService.extractUsername("valid-jwt")).thenReturn("test@email.com");
        when(userDetailsService.loadUserByUsername("test@email.com")).thenReturn(usuario);
        when(jwtService.isTokenValid("valid-jwt", usuario)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService).isTokenValid("valid-jwt", usuario);
    }

    @Test
    void shouldContinueChainWhenExceptionOccurs() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/mi-perfil");
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-jwt");

        when(sessionRepository.findByTokenAndExpiresAtAfter(any(), any()))
                .thenThrow(new RuntimeException("DB error"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
