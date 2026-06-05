package co.empresa.vivaeventos.auth.delivery.rest;

import co.empresa.vivaeventos.auth.domain.model.Dto.ActualizarPerfilRequest;
import co.empresa.vivaeventos.auth.domain.model.Dto.AuthResponse;
import co.empresa.vivaeventos.auth.domain.model.Dto.CambiarPasswordRequest;
import co.empresa.vivaeventos.auth.domain.model.Dto.LoginRequest;
import co.empresa.vivaeventos.auth.domain.model.Dto.RegistroRequest;
import co.empresa.vivaeventos.auth.domain.model.Usuario;
import co.empresa.vivaeventos.auth.domain.repository.ISessionRepository;
import co.empresa.vivaeventos.auth.domain.service.IUsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthRestController {

    private static final Logger log = LoggerFactory.getLogger(AuthRestController.class);

    private final IUsuarioService usuarioService;
    private final ISessionRepository sessionRepository;

    public AuthRestController(IUsuarioService usuarioService, ISessionRepository sessionRepository) {
        this.usuarioService = usuarioService;
        this.sessionRepository = sessionRepository;
    }

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("status", "ok");
        respuesta.put("message", "Auth service is running");
        respuesta.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/registro")
    public ResponseEntity<Map<String, Object>> registro(@RequestBody RegistroRequest request) {
        try {
            Usuario nuevo = usuarioService.save(request);
            AuthResponse authResponse = usuarioService.login(
                    new LoginRequest(request.getEmail(), request.getPassword())
            );

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("mensaje", "Usuario registrado con exito");
            respuesta.put("usuario", nuevo);
            respuesta.put("token", authResponse);

            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
        } catch (co.empresa.vivaeventos.auth.domain.exception.DatosInvalidosException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (co.empresa.vivaeventos.auth.domain.exception.UsuarioExistenteException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        } catch (Exception e) {
            log.error("Error en registro de usuario", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error interno del servidor");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        AuthResponse authResponse = usuarioService.login(request);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Login exitoso");
        respuesta.put("token", authResponse);
        
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", authResponse.getUserId() != null ? authResponse.getUserId().toString() : "");
        userMap.put("email", authResponse.getEmail() != null ? authResponse.getEmail() : "");
        userMap.put("fullName", authResponse.getFullName() != null ? authResponse.getFullName() : "");
        userMap.put("role", authResponse.getRole() != null ? authResponse.getRole() : "");
        userMap.put("phonePrefix", authResponse.getPhonePrefix() != null ? authResponse.getPhonePrefix() : "");
        userMap.put("phone", authResponse.getPhone() != null ? authResponse.getPhone() : "");
        userMap.put("documentType", authResponse.getDocumentType() != null ? authResponse.getDocumentType() : "");
        userMap.put("documentNumber", authResponse.getDocumentNumber() != null ? authResponse.getDocumentNumber() : "");
        userMap.put("country", authResponse.getCountry() != null ? authResponse.getCountry() : "");
        userMap.put("birthDate", authResponse.getBirthDate() != null ? authResponse.getBirthDate() : "");
        respuesta.put("user", userMap);

        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/mi-perfil")
    public ResponseEntity<Map<String, Object>> getMiPerfil(Authentication authentication) {
        String email = authentication.getName();
        Usuario usuario = usuarioService.findByEmail(email);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("usuario", usuario);

        return ResponseEntity.ok(respuesta);
    }

    @PutMapping("/mi-perfil")
    public ResponseEntity<Map<String, Object>> actualizarMiPerfil(
            Authentication authentication,
            @RequestBody ActualizarPerfilRequest request) {
        String email = authentication.getName();
        Usuario usuario = usuarioService.actualizarPerfil(email, request);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Perfil actualizado exitosamente");
        respuesta.put("usuario", usuario);

        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            sessionRepository.deleteByToken(token);
        }
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Sesion cerrada exitosamente");
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/solicitar-reset-password")
    public ResponseEntity<Map<String, Object>> solicitarResetPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Email requerido");
            return ResponseEntity.badRequest().body(error);
        }
        try {
            String token = usuarioService.solicitarResetPassword(email);
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("mensaje", "Si el email existe, recibiras un enlace de recuperacion");
            log.info("Password reset token for {}: {}", email, token);
            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("mensaje", "Si el email existe, recibiras un enlace de recuperacion");
            return ResponseEntity.ok(respuesta);
        }
    }

    @PostMapping("/restablecer-password")
    public ResponseEntity<Map<String, Object>> restablecerPassword(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String newPassword = body.get("newPassword");

        if (token == null || newPassword == null || newPassword.isBlank()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Token y nueva contrasena requeridos");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            usuarioService.restablecerPassword(token, newPassword);
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("mensaje", "Contrasena restablecida exitosamente");
            return ResponseEntity.ok(respuesta);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/cambiar-password")
    public ResponseEntity<Map<String, Object>> cambiarPassword(
            Authentication authentication,
            @RequestBody CambiarPasswordRequest request) {
        String email = authentication.getName();

        Map<String, Object> respuesta = new HashMap<>();
        try {
            boolean cerrarOtrasSesiones = request.isCerrarOtrasSesiones();
            usuarioService.cambiarPassword(
                    email,
                    request.getPasswordActual(),
                    request.getNuevaPassword(),
                    request.getConfirmarPassword(),
                    cerrarOtrasSesiones
            );
            respuesta.put("mensaje", "Contrasena cambiada exitosamente");
            return ResponseEntity.ok(respuesta);
        } catch (IllegalArgumentException e) {
            respuesta.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(respuesta);
        } catch (co.empresa.vivaeventos.auth.domain.exception.CredencialesInvalidasException e) {
            respuesta.put("error", "La contrasena actual no es correcta");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(respuesta);
        }
    }

    @GetMapping("/validar-email")
    public ResponseEntity<Map<String, Object>> validarEmail(@RequestParam String email) {
        boolean existe = usuarioService.existsByEmail(email);
        
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("existe", existe);
        
        return ResponseEntity.ok(respuesta);
    }
}