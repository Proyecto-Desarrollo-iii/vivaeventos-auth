package co.empresa.vivaeventos.auth.domain.exception;

import lombok.Getter;

@Getter
public class TwoFactorRequiredException extends RuntimeException {
    private final String tempToken;
    private final String userId;

    public TwoFactorRequiredException(String tempToken, String userId) {
        super("Se requiere autenticacion de dos factores");
        this.tempToken = tempToken;
        this.userId = userId;
    }
}
