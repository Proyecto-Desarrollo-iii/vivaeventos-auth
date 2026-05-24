package co.empresa.vivaeventos.auth.delivery.rest;

import co.empresa.vivaeventos.auth.config.JwtService;
import co.empresa.vivaeventos.auth.domain.exception.TwoFactorInvalidException;
import co.empresa.vivaeventos.auth.domain.model.Dto.AuthResponse;
import co.empresa.vivaeventos.auth.domain.model.Dto.TwoFactorDisableRequest;
import co.empresa.vivaeventos.auth.domain.model.Dto.TwoFactorLoginRequest;
import co.empresa.vivaeventos.auth.domain.model.Dto.TwoFactorSetupResponse;
import co.empresa.vivaeventos.auth.domain.model.Dto.TwoFactorVerifyRequest;
import co.empresa.vivaeventos.auth.domain.model.Session;
import co.empresa.vivaeventos.auth.domain.model.Usuario;
import co.empresa.vivaeventos.auth.domain.repository.ISessionRepository;
import co.empresa.vivaeventos.auth.domain.repository.IUsuarioRepository;
import co.empresa.vivaeventos.auth.domain.service.ITwoFactorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/2fa")
public class TwoFactorController {

    private static final Logger log = LoggerFactory.getLogger(TwoFactorController.class);

    private final ITwoFactorService twoFactorService;
    private final IUsuarioRepository usuarioRepository;
    private final ISessionRepository sessionRepository;
    private final JwtService jwtService;

    public TwoFactorController(ITwoFactorService twoFactorService,
                               IUsuarioRepository usuarioRepository,
                               ISessionRepository sessionRepository,
                               JwtService jwtService) {
        this.twoFactorService = twoFactorService;
        this.usuarioRepository = usuarioRepository;
        this.sessionRepository = sessionRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/setup")
    public ResponseEntity<Map<String, Object>> setup(Authentication authentication,
                                                      @RequestBody(required = false) Map<String, String> body) {
        String email = authentication.getName();
        String method = body != null ? body.getOrDefault("method", "APP") : "APP";

        if ("EMAIL".equals(method)) {
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            twoFactorService.sendEmailCode(usuario.getId(), email);
            twoFactorService.updateTwoFactorMethod(email, "EMAIL");

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("mensaje", "Codigo enviado a tu correo electronico");
            return ResponseEntity.ok(respuesta);
        }

        TwoFactorSetupResponse setup = twoFactorService.generateSetup(email);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Escanea el codigo QR con tu app de autenticacion");
        respuesta.put("secret", setup.getSecret());
        respuesta.put("qrCodeUri", setup.getQrCodeUri());
        respuesta.put("qrCodeBase64", setup.getQrCodeBase64());

        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/send-code")
    public ResponseEntity<Map<String, Object>> sendCode(@RequestBody Map<String, String> body) {
        String tempToken = body.get("tempToken");
        if (tempToken == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Token requerido");
            return ResponseEntity.badRequest().body(error);
        }
        try {
            if (!jwtService.isTemporaryToken(tempToken) || jwtService.isTokenExpired(tempToken)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Token invalido o expirado");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            String email = jwtService.extractUsername(tempToken);
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            twoFactorService.sendEmailCode(usuario.getId(), email);
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("mensaje", "Codigo enviado a tu correo");
            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            log.error("Error al enviar codigo 2FA", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error interno del servidor");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(Authentication authentication,
                                                       @RequestBody TwoFactorVerifyRequest request) {
        String email = authentication.getName();

        try {
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            if (usuario.getTwoFactorEnabled()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "2FA ya esta habilitado");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }

            boolean codeValid;
            if ("EMAIL".equals(usuario.getTwoFactorMethod())) {
                codeValid = twoFactorService.verifyEmailCode(usuario.getId(), request.getCode());
            } else {
                codeValid = twoFactorService.verifyCode(usuario.getTwoFactorSecret(), request.getCode());
            }

            if (!codeValid) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Codigo de verificacion invalido");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            usuario.setTwoFactorEnabled(true);
            usuarioRepository.save(usuario);

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("mensaje", "2FA habilitado exitosamente");
            return ResponseEntity.ok(respuesta);
        } catch (TwoFactorInvalidException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }
    }

    @PostMapping("/disable")
    public ResponseEntity<Map<String, Object>> disable(Authentication authentication,
                                                        @RequestBody TwoFactorDisableRequest request) {
        String email = authentication.getName();

        try {
            twoFactorService.disableTwoFactor(email, request.getPassword(), request.getCode());

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("mensaje", "2FA deshabilitado exitosamente");
            return ResponseEntity.ok(respuesta);
        } catch (TwoFactorInvalidException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (co.empresa.vivaeventos.auth.domain.exception.CredencialesInvalidasException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Contrasena incorrecta");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @PostMapping("/authenticate")
    public ResponseEntity<Map<String, Object>> authenticate2fa(@RequestBody TwoFactorLoginRequest request) {
        String tempToken = request.getTempToken();
        String code = request.getCode();

        if (tempToken == null || code == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Token y codigo requeridos");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            if (!jwtService.isTemporaryToken(tempToken)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Token invalido");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            String email = jwtService.extractUsername(tempToken);

            if (jwtService.isTokenExpired(tempToken)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Token expirado, inicie sesion nuevamente");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            boolean codeValid;
            if ("EMAIL".equals(usuario.getTwoFactorMethod())) {
                codeValid = twoFactorService.verifyEmailCode(usuario.getId(), code);
            } else {
                codeValid = twoFactorService.verifyCode(usuario.getTwoFactorSecret(), code);
            }

            if (!codeValid) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Codigo de verificacion invalido");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            String token = jwtService.generateToken(usuario);

            Session session = new Session();
            session.setUserId(usuario.getId());
            session.setToken(token);
            session.setExpiresAt(LocalDateTime.now().plusSeconds(jwtService.getExpirationSeconds()));
            sessionRepository.save(session);

            AuthResponse authResponse = new AuthResponse(
                    token, "Bearer",
                    usuario.getId().toString(), usuario.getEmail(),
                    usuario.getFullName(), usuario.getRole(),
                    usuario.getPhonePrefix(), usuario.getPhone(),
                    usuario.getDocumentType(), usuario.getDocumentNumber(),
                    usuario.getCountry(),
                    usuario.getBirthDate() != null ? usuario.getBirthDate().toString() : null,
                    true
            );

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("mensaje", "Autenticacion 2FA exitosa");
            respuesta.put("token", authResponse);

            Map<String, Object> userMap = buildUserMap(usuario);
            respuesta.put("user", userMap);

            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            log.error("Error en autenticacion 2FA", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error interno del servidor");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/change-method")
    public ResponseEntity<Map<String, Object>> changeMethod(Authentication authentication,
                                                             @RequestBody Map<String, String> body) {
        String email = authentication.getName();
        String method = body.get("method");
        if (method == null || (!"APP".equals(method) && !"EMAIL".equals(method))) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Metodo invalido. Use APP o EMAIL");
            return ResponseEntity.badRequest().body(error);
        }
        twoFactorService.updateTwoFactorMethod(email, method);
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Metodo 2FA actualizado a " + method);
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(Authentication authentication) {
        String email = authentication.getName();
        boolean enabled = twoFactorService.isTwoFactorEnabled(email);
        String method = twoFactorService.getTwoFactorMethod(email);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("enabled", enabled);
        respuesta.put("method", method);
        return ResponseEntity.ok(respuesta);
    }

    private Map<String, Object> buildUserMap(Usuario usuario) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", usuario.getId() != null ? usuario.getId().toString() : "");
        userMap.put("email", usuario.getEmail() != null ? usuario.getEmail() : "");
        userMap.put("fullName", usuario.getFullName() != null ? usuario.getFullName() : "");
        userMap.put("role", usuario.getRole() != null ? usuario.getRole() : "");
        userMap.put("phonePrefix", usuario.getPhonePrefix() != null ? usuario.getPhonePrefix() : "");
        userMap.put("phone", usuario.getPhone() != null ? usuario.getPhone() : "");
        userMap.put("documentType", usuario.getDocumentType() != null ? usuario.getDocumentType() : "");
        userMap.put("documentNumber", usuario.getDocumentNumber() != null ? usuario.getDocumentNumber() : "");
        userMap.put("country", usuario.getCountry() != null ? usuario.getCountry() : "");
        userMap.put("birthDate", usuario.getBirthDate() != null ? usuario.getBirthDate().toString() : "");
        return userMap;
    }
}
