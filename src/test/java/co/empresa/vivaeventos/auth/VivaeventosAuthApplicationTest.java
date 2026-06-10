package co.empresa.vivaeventos.auth;

import co.empresa.vivaeventos.auth.domain.repository.IPasswordResetTokenRepository;
import co.empresa.vivaeventos.auth.domain.repository.ISessionRepository;
import co.empresa.vivaeventos.auth.domain.repository.ITwoFactorCodeRepository;
import co.empresa.vivaeventos.auth.domain.repository.IUsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class VivaeventosAuthApplicationTest {

    @MockitoBean
    private IUsuarioRepository usuarioRepository;

    @MockitoBean
    private ISessionRepository sessionRepository;

    @MockitoBean
    private IPasswordResetTokenRepository passwordResetTokenRepository;

    @MockitoBean
    private ITwoFactorCodeRepository twoFactorCodeRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("application context should load")
    void contextLoads() {
        assertNotNull(applicationContext);
    }
}
