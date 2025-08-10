package model.home;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TxResponseDTO(
        Long id,
        String type,
        BigDecimal amount,
        Long categoryId,
        LocalDateTime txTime,
        String locationName,
        String note
) {
}
