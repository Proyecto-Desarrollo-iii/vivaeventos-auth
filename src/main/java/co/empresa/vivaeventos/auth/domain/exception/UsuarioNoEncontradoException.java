package co.empresa.vivaeventos.auth.domain.exception;

public class UsuarioNoEncontradoException extends RuntimeException {
    public UsuarioNoEncontradoException(String identifier) {
        super("Usuario no encontrado: " + identifier);
    }
}