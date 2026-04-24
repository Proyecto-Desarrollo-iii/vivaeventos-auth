package co.empresa.vivaeventos.auth.domain.model.Dto;

import lombok.Data;

@Data
public class RegistroRequest {
    private String email;
    private String password;
    private String fullName;
    private String role;
    private String phone;
}