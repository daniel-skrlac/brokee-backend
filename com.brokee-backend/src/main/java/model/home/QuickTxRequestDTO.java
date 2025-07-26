package model.home;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record QuickTxRequestDTO(
        @NotBlank
        @Pattern(regexp = "[EI]", message = "type must be 'E' (expense) or 'I' (income)")
        String type,

        @NotNull
        @Positive(message = "amount must be positive")
        BigDecimal amount,

        @NotNull
        @Positive(message = "categoryId must be a positive number")
        Long categoryId,

        @NotNull(message = "txTime cannot be null")
        OffsetDateTime txTime
) {
}
