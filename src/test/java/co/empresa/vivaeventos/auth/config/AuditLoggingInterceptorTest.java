package co.empresa.vivaeventos.auth.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLoggingInterceptorTest {

    @Mock
    private AuditEventClient auditEventClient;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private AuditLoggingInterceptor interceptor;

    @Captor
    private ArgumentCaptor<String> serviceNameCaptor;

    @Captor
    private ArgumentCaptor<String> userIdCaptor;

    @Captor
    private ArgumentCaptor<String> userRoleCaptor;

    @Captor
    private ArgumentCaptor<String> actionCaptor;

    @Captor
    private ArgumentCaptor<String> entityTypeCaptor;

    @Captor
    private ArgumentCaptor<String> entityIdCaptor;

    @Captor
    private ArgumentCaptor<String> newValuesCaptor;

    @Captor
    private ArgumentCaptor<String> ipAddressCaptor;

    @BeforeEach
    void setUp() {
        interceptor = new AuditLoggingInterceptor(auditEventClient);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void preHandleShouldSetStartTimeAttribute() {
        boolean result = interceptor.preHandle(request, response, null);

        assertTrue(result);
        verify(request).setAttribute(eq("auditStartTime"), anyLong());
    }

    @Test
    void afterCompletionShouldLogHttpRequestWithUserInfo() {
        UserDetails userDetails = new User("user@test.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(inv -> List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));
        when(request.getAttribute("auditStartTime")).thenReturn(1000L);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(response.getStatus()).thenReturn(200);

        interceptor.afterCompletion(request, response, null, null);

        verify(auditEventClient).logEvent(
                argThat(req ->
                        "auth".equals(req.serviceName()) &&
                        "user@test.com".equals(req.userId()) &&
                        "HTTP_REQUEST".equals(req.action()) &&
                        req.newValues().contains("200") &&
                        req.newValues().contains("login") &&
                        "10.0.0.1".equals(req.ipAddress())
                )
        );
    }

    @Test
    void afterCompletionShouldLogHttpRequestWithoutAuth() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);
        when(request.getAttribute("auditStartTime")).thenReturn(500L);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/registro");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("10.0.0.2");
        when(response.getStatus()).thenReturn(201);

        interceptor.afterCompletion(request, response, null, null);

        verify(auditEventClient).logEvent(
                argThat(req ->
                        "auth".equals(req.serviceName()) &&
                        req.userId() == null &&
                        req.userRole() == null &&
                        "HTTP_REQUEST".equals(req.action()) &&
                        "POST".equals(req.entityType()) &&
                        req.newValues().contains("201") &&
                        req.newValues().contains("registro") &&
                        "10.0.0.2".equals(req.ipAddress())
                )
        );
    }

    @Test
    void afterCompletionShouldSkipExcludedPaths() {
        when(request.getRequestURI()).thenReturn("/actuator/health");

        interceptor.afterCompletion(request, response, null, null);

        verify(auditEventClient, never()).logEvent(any());
    }
}
