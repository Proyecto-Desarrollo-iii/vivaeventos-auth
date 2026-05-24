package co.empresa.vivaeventos.auth.domain.repository;

import co.empresa.vivaeventos.auth.domain.model.TwoFactorCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ITwoFactorCodeRepository extends JpaRepository<TwoFactorCode, UUID> {
    Optional<TwoFactorCode> findByUserIdAndCodeAndUsedFalseAndExpiresAtAfter(UUID userId, String code, LocalDateTime now);
    void deleteByUserId(UUID userId);
}
