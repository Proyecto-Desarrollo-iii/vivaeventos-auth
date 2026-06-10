package co.empresa.vivaeventos.auth.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RequestLoggingFilterTest {

    @Test
    void shouldLogRequestAndProceed() throws Exception {
        RequestLoggingFilter filter = new RequestLoggingFilter();

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);

        Mockito.when(request.getMethod()).thenReturn("GET");
        Mockito.when(request.getRequestURI()).thenReturn("/api/test");
        Mockito.when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        Mockito.when(request.getHeader("Authorization")).thenReturn("Bearer test");

        filter.doFilterInternal(request, response, chain);

        Mockito.verify(chain).doFilter(request, response);
    }
}
