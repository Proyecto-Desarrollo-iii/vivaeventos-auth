package co.empresa.vivaeventos.auth.domain.model.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private String userId;
    private String email;
    private String fullName;
    private String role;
    private String phonePrefix;
    private String phone;
    private String documentType;
    private String documentNumber;
    private String country;
    private String birthDate;
    private boolean twoFactorEnabled;
}