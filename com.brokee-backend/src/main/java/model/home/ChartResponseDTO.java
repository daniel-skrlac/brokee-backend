package model.home;

import java.math.BigDecimal;
import java.util.List;

public record ChartResponseDTO(
        List<String> labels,
        List<BigDecimal> values
) {
}