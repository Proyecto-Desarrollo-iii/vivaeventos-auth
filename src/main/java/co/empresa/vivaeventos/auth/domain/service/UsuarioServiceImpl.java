package co.empresa.vivaeventos.auth.domain.service;

import co.empresa.vivaeventos.auth.domain.exception.CredencialesInvalidasException;
import co.empresa.vivaeventos.auth.domain.exception.UsuarioExistenteException;
import co.empresa.vivaeventos.auth.domain.exception.UsuarioNoEncontradoException;
import co.empresa.vivaeventos.auth.domain.model.Dto.AuthResponse;
import co.empresa.vivaeventos.auth.domain.model.Dto.LoginRequest;
import co.empresa.vivaeventos.auth.domain.model.Dto.RegistroRequest;
import co.empresa.vivaeventos.auth.domain.model.Usuario;
import co.empresa.vivaeventos.auth.domain.repository.IUsuarioRepository;
import co.empresa.vivaeventos.auth.config.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UsuarioServiceImpl implements IUsuarioService {

    private final IUsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public UsuarioServiceImpl(
            IUsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario findById(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario findByEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsuarioNoEncontradoException(email));
    }

    @Override
    @Transactional
    public Usuario save(RegistroRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new UsuarioExistenteException(request.getEmail());
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(request.getEmail());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setFullName(request.getFullName());
        
        // Mapear el rol a los valores válidos de la base de datos
        String role = request.getRole() != null ? request.getRole().toUpperCase() : "CLIENT";
        // Mapeo de variantes en español/otros a los valores del CHECK constraint
        if (role.equals("CLIENTE") || role.equals("CLIENT")) {
            role = "CLIENT";
        } else if (role.equals("ORGANIZADOR") || role.equals("ORGANIZER")) {
            role = "ORGANIZER";
        } else if (role.equals("ADMIN") || role.equals("ADMINISTRADOR")) {
            role = "ADMIN";
        } else if (role.equals("LOGISTICA") || role.equals("LOGISTICS")) {
            role = "LOGISTICS";
        } else {
            role = "CLIENT"; // Valor por defecto
        }
        usuario.setRole(role);
        
        usuario.setPhone(request.getPhone());
        usuario.setIsActive(true);

        return usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        Usuario usuario = findByEmail(request.getEmail());
        
        if (usuario.getIsActive() == null || !usuario.getIsActive()) {
            throw new CredencialesInvalidasException();
        }

        String token = jwtService.generateToken(usuario);

        return new AuthResponse(
                token,
                "Bearer",
                usuario.getId().toString(),
                usuario.getEmail(),
                usuario.getFullName(),
                usuario.getRole()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }
}