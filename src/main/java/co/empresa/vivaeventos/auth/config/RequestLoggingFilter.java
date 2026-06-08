package co.empresa.vivaeventos.auth.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("=== INCOMING REQUEST ===");
        log.info("Method: {}", request.getMethod());
        log.info("URI: {}", request.getRequestURI());
        log.info("URL: {}", request.getRequestURL());
        log.info("Query: {}", request.getQueryString());
        log.info("Headers: ");
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            log.info("  {}: {}", headerName, request.getHeader(headerName));
        }
        log.info("======================");
        filterChain.doFilter(request, response);
    }
}
