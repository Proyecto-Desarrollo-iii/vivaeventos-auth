package co.empresa.vivaeventos.auth.delivery.rest;

import co.empresa.vivaeventos.auth.domain.model.Dto.AuthResponse;
import co.empresa.vivaeventos.auth.domain.model.Dto.LoginRequest;
import co.empresa.vivaeventos.auth.domain.model.Dto.RegistroRequest;
import co.empresa.vivaeventos.auth.domain.model.Usuario;
import co.empresa.vivaeventos.auth.domain.service.IUsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthRestController {

    private final IUsuarioService usuarioService;

    public AuthRestController(IUsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("status", "ok");
        respuesta.put("message", "Auth service is running");
        respuesta.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/debug-registro")
    public ResponseEntity<Map<String, Object>> debugRegistro(@RequestBody RegistroRequest request) {
        Map<String, Object> respuesta = new HashMap<>();
        try {
            respuesta.put("received", request);
            respuesta.put("email", request.getEmail());
            respuesta.put("hasEmail", request.getEmail() != null);
            respuesta.put("hasPassword", request.getPassword() != null);
            respuesta.put("hasFullName", request.getFullName() != null);
            respuesta.put("hasRole", request.getRole() != null);
            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            respuesta.put("error", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(respuesta);
        }
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
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("type", e.getClass().getName());
            e.printStackTrace();
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
        respuesta.put("user", userMap);

        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/mi-perfil")
    public ResponseEntity<Map<String, Object>> getMiPerfil(@RequestParam String email) {
        Usuario usuario = usuarioService.findByEmail(email);
        
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("usuario", usuario);
        
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/validar-email")
    public ResponseEntity<Map<String, Object>> validarEmail(@RequestParam String email) {
        boolean existe = usuarioService.existsByEmail(email);
        
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("existe", existe);
        
        return ResponseEntity.ok(respuesta);
    }
}