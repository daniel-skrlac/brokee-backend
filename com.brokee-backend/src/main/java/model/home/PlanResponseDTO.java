package model.home;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PlanResponseDTO(
        Long id,
        String title,
        BigDecimal amount,
        LocalDate dueDate
) {
}
