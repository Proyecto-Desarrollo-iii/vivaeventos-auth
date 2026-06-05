package co.empresa.vivaeventos.auth.domain.exception;

public class UsuarioExistenteException extends RuntimeException {
    public UsuarioExistenteException(String email) {
        super("Ya existe una cuenta registrada con el correo " + email + ". Intenta iniciar sesion.");
    }
}