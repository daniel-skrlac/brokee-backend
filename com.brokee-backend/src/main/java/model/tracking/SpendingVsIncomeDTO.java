package model.tracking;

import java.math.BigDecimal;

public record SpendingVsIncomeDTO(
        String month,
        BigDecimal expenses,
        BigDecimal income
) {
}
