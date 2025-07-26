package model.home;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record FullTxRequestDTO(
        String type,
        BigDecimal amount,
        Long categoryId,
        OffsetDateTime txTime,
        BigDecimal latitude,
        BigDecimal longitude,
        String merchant,
        String note
) {
}
