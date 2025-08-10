package model.tracking;

import java.math.BigDecimal;

public record CategoryBreakdownDTO(
        String category,
        BigDecimal amount
) {
}
