package co.empresa.vivaeventos.auth.domain.model.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class RegistroRequest {
    private String email;
    private String password;
    private String fullName;
    private String role;
    private String phonePrefix;
    private String phone;
    private String documentType;
    private String documentNumber;
    private String country;
    private LocalDate birthDate;
}