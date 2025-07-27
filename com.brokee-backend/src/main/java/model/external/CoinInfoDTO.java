package model.external;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoinInfoDTO {
    private String symbol;
    private BigDecimal free;
    private BigDecimal locked;
    private List<TradeDTO> trades;

    public CoinInfoDTO() {
    }

    public CoinInfoDTO(String symbol,
                       BigDecimal free,
                       BigDecimal locked,
                       List<TradeDTO> trades) {
        this.symbol = symbol;
        this.free = free;
        this.locked = locked;
        this.trades = trades;
    }

}

