package co.empresa.vivaeventos.auth.config;

import co.empresa.vivaeventos.auth.domain.repository.ISessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class SessionCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(SessionCleanupTask.class);

    private final ISessionRepository sessionRepository;

    public SessionCleanupTask(ISessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void deleteExpiredSessions() {
        int deleted = sessionRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired sessions", deleted);
        }
    }
}
