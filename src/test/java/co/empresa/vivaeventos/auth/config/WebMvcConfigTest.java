package co.empresa.vivaeventos.auth.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebMvcConfigTest {

    @Mock
    private AuditLoggingInterceptor auditLoggingInterceptor;

    @Mock
    private InterceptorRegistry interceptorRegistry;

    @Mock
    private InterceptorRegistration interceptorRegistration;

    @InjectMocks
    private WebMvcConfig webMvcConfig;

    @Test
    void shouldRegisterAuditLoggingInterceptor() {
        when(interceptorRegistry.addInterceptor(any())).thenReturn(interceptorRegistration);

        webMvcConfig.addInterceptors(interceptorRegistry);

        verify(interceptorRegistry).addInterceptor(auditLoggingInterceptor);
    }
}
