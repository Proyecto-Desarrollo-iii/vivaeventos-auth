package co.empresa.vivaeventos.auth.domain.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TwoFactorStatusResponse {
    private boolean enabled;
}
