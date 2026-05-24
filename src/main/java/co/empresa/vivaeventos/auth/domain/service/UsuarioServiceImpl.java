package co.empresa.vivaeventos.auth.domain.service;

import co.empresa.vivaeventos.auth.domain.exception.CredencialesInvalidasException;
import co.empresa.vivaeventos.auth.domain.exception.TwoFactorRequiredException;
import co.empresa.vivaeventos.auth.domain.exception.UsuarioExistenteException;
import co.empresa.vivaeventos.auth.domain.exception.UsuarioNoEncontradoException;
import co.empresa.vivaeventos.auth.domain.model.Dto.ActualizarPerfilRequest;
import co.empresa.vivaeventos.auth.domain.model.Dto.AuthResponse;
import co.empresa.vivaeventos.auth.domain.model.Dto.LoginRequest;
import co.empresa.vivaeventos.auth.domain.model.Dto.RegistroRequest;
import co.empresa.vivaeventos.auth.domain.model.PasswordResetToken;
import co.empresa.vivaeventos.auth.domain.model.Session;
import co.empresa.vivaeventos.auth.domain.model.Usuario;
import co.empresa.vivaeventos.auth.domain.repository.IPasswordResetTokenRepository;
import co.empresa.vivaeventos.auth.domain.repository.IUsuarioRepository;
import co.empresa.vivaeventos.auth.domain.repository.ISessionRepository;
import co.empresa.vivaeventos.auth.config.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UsuarioServiceImpl implements IUsuarioService {

    private final IUsuarioRepository usuarioRepository;
    private final ISessionRepository sessionRepository;
    private final IPasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public UsuarioServiceImpl(
            IUsuarioRepository usuarioRepository,
            ISessionRepository sessionRepository,
            IPasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager) {
        this.usuarioRepository = usuarioRepository;
        this.sessionRepository = sessionRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
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
            role = "LOGISTICA";
        } else {
            role = "CLIENT"; // Valor por defecto
        }
        usuario.setRole(role);
        
        usuario.setPhonePrefix(request.getPhonePrefix());
        usuario.setPhone(request.getPhone());
        usuario.setDocumentType(request.getDocumentType());
        usuario.setDocumentNumber(request.getDocumentNumber());
        usuario.setCountry(request.getCountry());
        usuario.setBirthDate(request.getBirthDate());
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

        if (Boolean.TRUE.equals(usuario.getTwoFactorEnabled())) {
            String tempToken = jwtService.generateTemporaryToken(usuario);
            throw new TwoFactorRequiredException(tempToken, usuario.getId().toString());
        }

        String token = jwtService.generateToken(usuario);

        Session session = new Session();
        session.setUserId(usuario.getId());
        session.setToken(token);
        session.setExpiresAt(LocalDateTime.now().plusSeconds(jwtService.getExpirationSeconds()));
        sessionRepository.save(session);

        return new AuthResponse(
                token,
                "Bearer",
                usuario.getId().toString(),
                usuario.getEmail(),
                usuario.getFullName(),
                usuario.getRole(),
                usuario.getPhonePrefix(),
                usuario.getPhone(),
                usuario.getDocumentType(),
                usuario.getDocumentNumber(),
                usuario.getCountry(),
                usuario.getBirthDate() != null ? usuario.getBirthDate().toString() : null,
                Boolean.TRUE.equals(usuario.getTwoFactorEnabled())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    public String solicitarResetPassword(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsuarioNoEncontradoException(email));

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUserId(usuario.getId());
        resetToken.setToken(UUID.randomUUID().toString());
        resetToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        passwordResetTokenRepository.save(resetToken);

        return resetToken.getToken();
    }

    @Override
    @Transactional
    public void restablecerPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token de recuperacion invalido"));

        if (resetToken.isExpired()) {
            throw new RuntimeException("Token de recuperacion expirado");
        }

        if (resetToken.isUsed()) {
            throw new RuntimeException("Token de recuperacion ya utilizado");
        }

        Usuario usuario = usuarioRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new UsuarioNoEncontradoException(resetToken.getUserId().toString()));

        usuario.setPassword(passwordEncoder.encode(newPassword));
        usuarioRepository.save(usuario);

        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);
    }

    @Override
    @Transactional
    public Usuario actualizarPerfil(String email, ActualizarPerfilRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsuarioNoEncontradoException(email));

        if (request.getFullName() != null) {
            usuario.setFullName(request.getFullName());
        }
        if (request.getPhonePrefix() != null) {
            usuario.setPhonePrefix(request.getPhonePrefix());
        }
        if (request.getPhone() != null) {
            usuario.setPhone(request.getPhone());
        }
        if (request.getDocumentType() != null) {
            usuario.setDocumentType(request.getDocumentType());
        }
        if (request.getDocumentNumber() != null) {
            usuario.setDocumentNumber(request.getDocumentNumber());
        }
        if (request.getCountry() != null) {
            usuario.setCountry(request.getCountry());
        }
        if (request.getBirthDate() != null) {
            usuario.setBirthDate(request.getBirthDate());
        }

        return usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void cambiarPassword(String email, String passwordActual, String nuevaPassword, String confirmarPassword, boolean cerrarOtrasSesiones) {
        if (!nuevaPassword.equals(confirmarPassword)) {
            throw new IllegalArgumentException("Las contrasenas no coinciden");
        }

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsuarioNoEncontradoException(email));

        if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
            throw new CredencialesInvalidasException();
        }

        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);

        if (cerrarOtrasSesiones) {
            sessionRepository.deleteByUserId(usuario.getId());
        }
    }
}