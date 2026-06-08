package co.empresa.vivaeventos.auth.config;

import co.empresa.vivaeventos.auth.domain.repository.ISessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionCleanupTaskTest {

    @Mock
    private ISessionRepository sessionRepository;

    @InjectMocks
    private SessionCleanupTask sessionCleanupTask;

    @Test
    void shouldDeleteExpiredSessions() {
        when(sessionRepository.deleteByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(5);

        sessionCleanupTask.deleteExpiredSessions();

        verify(sessionRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));
    }

    @Test
    void shouldHandleZeroExpiredSessions() {
        when(sessionRepository.deleteByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(0);

        sessionCleanupTask.deleteExpiredSessions();

        verify(sessionRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));
    }
}
