package model.external;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevolutTransactionDTO(
        LocalDate date,
        String description,
        BigDecimal sentAmount,
        BigDecimal receivedAmount
) {}