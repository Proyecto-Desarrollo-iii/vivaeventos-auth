package co.empresa.vivaeventos.auth.domain.service;

import co.empresa.vivaeventos.auth.domain.model.dto.TwoFactorSetupResponse;

import java.util.UUID;

public interface ITwoFactorService {
    TwoFactorSetupResponse generateSetup(String email);
    boolean verifyCode(String secret, String code);
    void enableTwoFactor(String email, String code);
    void disableTwoFactor(String email, String password, String code);
    boolean isTwoFactorEnabled(String email);
    String sendEmailCode(UUID userId, String email);
    boolean verifyEmailCode(UUID userId, String code);
    void updateTwoFactorMethod(String email, String method);
    String getTwoFactorMethod(String email);
}
