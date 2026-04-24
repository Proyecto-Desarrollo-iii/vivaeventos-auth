package co.empresa.vivaeventos.auth.domain.exception;

public class UsuarioExistenteException extends RuntimeException {
    public UsuarioExistenteException(String email) {
        super("Ya existe un usuario con el email: " + email);
    }
}