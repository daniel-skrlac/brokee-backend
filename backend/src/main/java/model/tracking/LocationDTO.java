package model.tracking;

import java.math.BigDecimal;

public record LocationDTO(
        BigDecimal latitude,
        BigDecimal longitude,
        String label,
        BigDecimal amount
) {
}
