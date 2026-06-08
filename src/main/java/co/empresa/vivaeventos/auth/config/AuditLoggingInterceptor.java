package co.empresa.vivaeventos.auth.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

@Component
public class AuditLoggingInterceptor implements HandlerInterceptor {

    private static final List<String> EXCLUDED_PATHS = Arrays.asList("/actuator", "/error");

    private final AuditEventClient auditEventClient;

    public AuditLoggingInterceptor(AuditEventClient auditEventClient) {
        this.auditEventClient = auditEventClient;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("auditStartTime", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String path = request.getRequestURI();
        if (isExcluded(path)) return;

        long startTime = (long) request.getAttribute("auditStartTime");
        long duration = System.currentTimeMillis() - startTime;

        String userId = null;
        String userRole = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            Object principal = auth.getPrincipal();
            if (principal instanceof UserDetails userDetails) {
                userId = userDetails.getUsername();
            }
            if (auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
                userRole = auth.getAuthorities().iterator().next().getAuthority();
                if (userRole != null && userRole.startsWith("ROLE_")) {
                    userRole = userRole.substring(5);
                }
            }
        }

        String ip = request.getRemoteAddr();
        String method = request.getMethod();
        int status = response.getStatus();

        String newValues = "{\"method\":\"" + method + "\",\"path\":\"" + path + "\",\"status\":" + status + ",\"durationMs\":" + duration + "}";

        auditEventClient.logEvent(new AuditEventRequest("auth", userId, userRole, "HTTP_REQUEST", method, null, newValues, ip));
    }

    private boolean isExcluded(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }
}
