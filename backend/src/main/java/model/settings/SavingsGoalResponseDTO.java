package model.settings;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
public class SavingsGoalResponseDTO {
    public String userSub;
    public BigDecimal targetAmt;
    public LocalDate targetDate;
}