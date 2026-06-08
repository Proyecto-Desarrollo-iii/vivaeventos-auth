package co.empresa.vivaeventos.auth.domain.service;

import co.empresa.vivaeventos.auth.domain.model.dto.ActualizarPerfilRequest;
import co.empresa.vivaeventos.auth.domain.model.dto.AuthResponse;
import co.empresa.vivaeventos.auth.domain.model.dto.LoginRequest;
import co.empresa.vivaeventos.auth.domain.model.dto.RegistroRequest;
import co.empresa.vivaeventos.auth.domain.model.Usuario;

import java.util.UUID;

public interface IUsuarioService {
    Usuario findById(UUID id);
    Usuario findByEmail(String email);
    Usuario save(RegistroRequest request);
    AuthResponse login(LoginRequest request);
    boolean existsByEmail(String email);
    String solicitarResetPassword(String email);
    void restablecerPassword(String token, String newPassword);
    Usuario actualizarPerfil(String email, ActualizarPerfilRequest request);
    void cambiarPassword(String email, String passwordActual, String nuevaPassword, String confirmarPassword, boolean cerrarOtrasSesiones);
}