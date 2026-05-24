package co.empresa.vivaeventos.auth.domain.exception;

public class TwoFactorInvalidException extends RuntimeException {
    public TwoFactorInvalidException() {
        super("Codigo de verificacion invalido");
    }
}
