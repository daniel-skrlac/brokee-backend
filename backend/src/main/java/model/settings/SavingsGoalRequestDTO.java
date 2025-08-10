package model.settings;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
public class SavingsGoalRequestDTO {
    @NotNull
    @DecimalMin("0.00")
    public BigDecimal targetAmt;

    @NotNull
    public LocalDate targetDate;
}
