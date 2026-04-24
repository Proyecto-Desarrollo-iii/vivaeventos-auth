package co.empresa.vivaeventos.auth.domain.service;

import co.empresa.vivaeventos.auth.domain.model.Dto.AuthResponse;
import co.empresa.vivaeventos.auth.domain.model.Dto.LoginRequest;
import co.empresa.vivaeventos.auth.domain.model.Dto.RegistroRequest;
import co.empresa.vivaeventos.auth.domain.model.Usuario;

import java.util.UUID;

public interface IUsuarioService {
    Usuario findById(UUID id);
    Usuario findByEmail(String email);
    Usuario save(RegistroRequest request);
    AuthResponse login(LoginRequest request);
    boolean existsByEmail(String email);
}