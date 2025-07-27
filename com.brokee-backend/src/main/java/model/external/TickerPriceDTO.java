package model.external;

import java.math.BigDecimal;

public class TickerPriceDTO {
    public String symbol;
    public BigDecimal lastPrice;
    public BigDecimal volume;
    public BigDecimal priceChangePercent;
    public BigDecimal quoteVolume;
    public BigDecimal highPrice;
    public BigDecimal lowPrice;
    public BigDecimal openPrice;
}
