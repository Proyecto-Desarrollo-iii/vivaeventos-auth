package co.empresa.vivaeventos.auth.domain.model.dto;

import lombok.Data;

@Data
public class TwoFactorVerifyRequest {
    private String code;
}
