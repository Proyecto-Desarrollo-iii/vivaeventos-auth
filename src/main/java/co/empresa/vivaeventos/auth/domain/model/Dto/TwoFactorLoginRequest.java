package co.empresa.vivaeventos.auth.domain.model.Dto;

import lombok.Data;

@Data
public class TwoFactorLoginRequest {
    private String tempToken;
    private String code;
}
