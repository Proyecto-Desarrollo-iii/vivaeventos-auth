package co.empresa.vivaeventos.auth.domain.model.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ActualizarPerfilRequest {
    private String fullName;
    private String phonePrefix;
    private String phone;
    private String documentType;
    private String documentNumber;
    private String country;
    private LocalDate birthDate;
}
