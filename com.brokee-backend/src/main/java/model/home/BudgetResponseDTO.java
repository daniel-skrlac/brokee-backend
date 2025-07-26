package model.home;

import java.math.BigDecimal;

public record BudgetResponseDTO(
        Long categoryId,
        BigDecimal amount
) {
}
