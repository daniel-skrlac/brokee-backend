package model.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PlannedTxResponseDTO {
    public Long id;
    public String type;
    public Long categoryId;
    public String title;
    public BigDecimal amount;
    public LocalDate dueDate;
    public Boolean autoBook;
}
