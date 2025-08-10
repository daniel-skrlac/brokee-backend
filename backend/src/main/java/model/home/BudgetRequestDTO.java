package model.home;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record BudgetRequestDTO(
        @NotNull Long categoryId,
        @NotNull @Positive BigDecimal amount
) {
}
