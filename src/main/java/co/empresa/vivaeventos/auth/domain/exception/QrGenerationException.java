package co.empresa.vivaeventos.auth.domain.exception;

public class QrGenerationException extends RuntimeException {
    public QrGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
