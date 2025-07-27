package model.external;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class TopCoinDTO {
    private String symbol;
    private BigDecimal lastPrice;
    private BigDecimal priceChangePercent;
    private BigDecimal volume;
    private BigDecimal quoteVolume;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
}
