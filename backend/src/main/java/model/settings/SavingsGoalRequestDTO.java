package model.settings;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SavingsGoalRequestDTO {
    @NotNull
    @DecimalMin("0.00")
    public BigDecimal targetAmt;

    @NotNull
    public LocalDate targetDate;
}
