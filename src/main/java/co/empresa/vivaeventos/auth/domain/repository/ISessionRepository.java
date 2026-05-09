package co.empresa.vivaeventos.auth.domain.repository;

import co.empresa.vivaeventos.auth.domain.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ISessionRepository extends JpaRepository<Session, UUID> {
    Optional<Session> findByTokenAndExpiresAtAfter(String token, LocalDateTime now);
    int deleteByToken(String token);
    int deleteByExpiresAtBefore(LocalDateTime before);
    int deleteByUserId(UUID userId);
}
