package model.home;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BudgetRequestDTO(
        @NotNull Long categoryId,
        @NotNull @DecimalMin("0.00") BigDecimal amount
) {
}
