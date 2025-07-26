package model.tracking;

import java.math.BigDecimal;

public record DashboardSummaryDTO(
        BigDecimal totalExpenses,
        BigDecimal totalIncome,
        BigDecimal budgetRemaining
) {
}
