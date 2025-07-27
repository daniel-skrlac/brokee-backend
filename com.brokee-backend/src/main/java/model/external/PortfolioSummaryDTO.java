package model.external;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class PortfolioSummaryDTO {
    private List<TopCoinDTO> topCoins;
    private List<CoinInfoDTO> holdings;
    private BigDecimal totalEurValue;
}
