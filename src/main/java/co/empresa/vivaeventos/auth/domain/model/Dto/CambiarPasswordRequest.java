package co.empresa.vivaeventos.auth.domain.model.Dto;

import lombok.Data;

@Data
public class CambiarPasswordRequest {
    private String passwordActual;
    private String nuevaPassword;
    private String confirmarPassword;
    private boolean cerrarOtrasSesiones;
}
