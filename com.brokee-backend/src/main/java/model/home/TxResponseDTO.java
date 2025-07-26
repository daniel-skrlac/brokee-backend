package model.home;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TxResponseDTO(
        Long id,
        String type,
        BigDecimal amount,
        Long categoryId,
        OffsetDateTime txTime,
        String merchant,
        String note
) {
}
