package model.external;

import java.math.BigDecimal;

public record HoldingDTO(String asset, BigDecimal free, BigDecimal locked, BigDecimal totalValue) {
}
