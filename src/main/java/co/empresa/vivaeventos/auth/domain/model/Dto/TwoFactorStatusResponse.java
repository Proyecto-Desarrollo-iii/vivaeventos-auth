package co.empresa.vivaeventos.auth.domain.model.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TwoFactorStatusResponse {
    private boolean enabled;
}
