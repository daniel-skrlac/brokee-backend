package model.home;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record QuickTxRequestDTO(
        String type,
        BigDecimal amount,
        Long categoryId,
        OffsetDateTime txTime
) {
}
