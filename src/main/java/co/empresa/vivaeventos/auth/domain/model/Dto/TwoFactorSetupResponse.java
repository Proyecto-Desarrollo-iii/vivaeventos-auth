package co.empresa.vivaeventos.auth.domain.model.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TwoFactorSetupResponse {
    private String secret;
    private String qrCodeUri;
    private String qrCodeBase64;
}
