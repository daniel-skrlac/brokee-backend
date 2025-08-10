package model.home;

import java.math.BigDecimal;

public record DashboardSummaryResponseDTO(
        BigDecimal balance,
        double budgetUsagePercent,
        long scheduledCount
) {
}