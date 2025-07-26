package model.home;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PlanRequestDTO(
        String title,
        BigDecimal amount,
        LocalDate dueDate
) {
}
