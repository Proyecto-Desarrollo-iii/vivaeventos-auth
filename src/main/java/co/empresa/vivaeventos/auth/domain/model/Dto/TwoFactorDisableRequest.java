package co.empresa.vivaeventos.auth.domain.model.Dto;

import lombok.Data;

@Data
public class TwoFactorDisableRequest {
    private String password;
    private String code;
}
