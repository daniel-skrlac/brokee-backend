package model.external;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class FullPortfolioDTO {
    private List<TopCoinDTO> topMarketCoins;
    private List<CoinPortfolioEntryDTO> myCoins;
    private BigDecimal totalEurValue;
}
