package co.empresa.vivaeventos.auth.config;

import co.empresa.vivaeventos.auth.domain.repository.ISessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    classes = SecurityConfig.class,
    webEnvironment = WebEnvironment.MOCK
)
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
class SecurityConfigTest {

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private RequestLoggingFilter requestLoggingFilter;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private ISessionRepository sessionRepository;

    @Autowired
    private SecurityConfig securityConfig;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private AuthenticationProvider authenticationProvider;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Test
    void passwordEncoderShouldReturnBCryptPasswordEncoder() {
        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void passwordEncoderShouldEncodePasswords() {
        String rawPassword = "miPassword123";
        String encoded = passwordEncoder.encode(rawPassword);
        assertThat(encoded).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, encoded)).isTrue();
    }

    @Test
    void corsConfigurationSourceShouldBeUrlBased() {
        assertThat(corsConfigurationSource).isInstanceOf(UrlBasedCorsConfigurationSource.class);
    }

    @Test
    void corsConfigurationShouldAllowExpectedOriginPatterns() {
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;
        CorsConfiguration config = source.getCorsConfigurations().get("/**");
        assertThat(config).isNotNull();
        assertThat(config.getAllowedOriginPatterns())
            .contains(
                "http://localhost:*",
                "https://localhost:*",
                "http://192.168.*.*:*",
                "https://192.168.*.*:*",
                "https://*.devtunnels.ms",
                "https://*.devtunnels.ms:*"
            );
    }

    @Test
    void corsConfigurationShouldAllowExpectedMethods() {
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;
        CorsConfiguration config = source.getCorsConfigurations().get("/**");
        assertThat(config.getAllowedMethods())
            .contains("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH");
    }

    @Test
    void corsConfigurationShouldAllowAllHeaders() {
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;
        CorsConfiguration config = source.getCorsConfigurations().get("/**");
        assertThat(config.getAllowedHeaders()).contains("*");
    }

    @Test
    void corsConfigurationShouldExposeAllHeaders() {
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;
        CorsConfiguration config = source.getCorsConfigurations().get("/**");
        assertThat(config.getExposedHeaders()).contains("*");
    }

    @Test
    void corsConfigurationShouldNotAllowCredentials() {
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;
        CorsConfiguration config = source.getCorsConfigurations().get("/**");
        assertThat(config.getAllowCredentials()).isFalse();
    }

    @Test
    void corsConfigurationShouldHaveMaxAge() {
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;
        CorsConfiguration config = source.getCorsConfigurations().get("/**");
        assertThat(config.getMaxAge()).isEqualTo(3600L);
    }

    @Test
    void corsConfigurationShouldApplyToAllPaths() {
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;
        assertThat(source.getCorsConfigurations()).containsKey("/**");
    }

    @Test
    void jwtAuthenticationFilterShouldBeCreated() {
        assertThat(jwtAuthenticationFilter).isNotNull();
    }

    @Test
    void authenticationProviderShouldBeDaoAuthenticationProvider() {
        assertThat(authenticationProvider).isInstanceOf(DaoAuthenticationProvider.class);
    }

    @Test
    void authenticationManagerShouldBeCreated() {
        assertThat(authenticationManager).isNotNull();
    }

    @Test
    void securityFilterChainShouldBeCreated() {
        assertThat(securityFilterChain).isNotNull();
    }
}
