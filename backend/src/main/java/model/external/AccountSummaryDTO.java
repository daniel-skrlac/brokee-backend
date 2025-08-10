package model.external;

import java.util.List;

public record AccountSummaryDTO(List<HoldingDTO> topHoldings, List<TopCoinDTO> topCoins) {

}