package co.empresa.vivaeventos.auth.domain.model.Dto;

import lombok.Data;

@Data
public class TwoFactorVerifyRequest {
    private String code;
}
