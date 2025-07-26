package model.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PlannedTxRequestDTO {
    @NotBlank
    @Pattern(regexp = "[EI]")
    public String type;

    @NotNull
    public Long categoryId;

    @NotBlank
    public String title;

    @NotNull
    @DecimalMin("0.01")
    public BigDecimal amount;

    @NotNull
    public LocalDate dueDate;

    public Boolean autoBook;
}
