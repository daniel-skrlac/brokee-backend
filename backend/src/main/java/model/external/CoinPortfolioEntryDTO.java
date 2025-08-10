package model.external;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class CoinPortfolioEntryDTO {
    private String symbol;
    private BigDecimal free;
    private BigDecimal locked;
    private BigDecimal eurValue;
    private List<TradeDTO> trades;
}
