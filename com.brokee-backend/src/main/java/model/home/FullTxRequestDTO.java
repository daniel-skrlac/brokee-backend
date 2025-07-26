package model.home;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record FullTxRequestDTO(
        @NotBlank @Pattern(regexp = "[EI]") String type,
        @NotNull @Positive BigDecimal amount,
        @NotNull @Positive Long categoryId,
        @NotNull OffsetDateTime txTime,
        @Size(max = 255) String merchant,
        @Size(max = 500) String note,
        @DecimalMin(value = "-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude
) {
}
