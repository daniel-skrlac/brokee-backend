package service;

import client.BinanceClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import model.entity.BinanceToken;
import model.external.BinanceAccountDTO;
import model.external.CoinPortfolioEntryDTO;
import model.external.FullPortfolioDTO;
import model.external.TickerPriceDTO;
import model.external.TopCoinDTO;
import model.external.TradeDTO;
import model.response.ServiceResponse;
import model.response.ServiceResponseDirector;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import repository.BinanceTokenRepository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class BinanceService {
    @Inject
    @RestClient
    BinanceClient client;

    @Inject
    BinanceTokenRepository tokenRepo;

    @Transactional
    public ServiceResponse<BinanceToken> saveCredentials(String userSub, String apiKey, String secretKey) {
        BinanceToken token = tokenRepo.findByUserSub(userSub)
                .map(t -> tokenRepo.updateKeys(t.getId(), apiKey, secretKey))
                .orElseGet(() -> tokenRepo.createForUser(userSub, apiKey, secretKey));
        return ServiceResponseDirector.successOk(token, "Credentials saved");
    }

    public ServiceResponse<BinanceToken> getCredentials(String userSub) {
        return tokenRepo.findByUserSub(userSub)
                .map(token -> ServiceResponseDirector.successOk(token, "OK"))
                .orElseGet(() -> ServiceResponseDirector.errorNotFound(
                        "No Binance credentials stored for user: " + userSub
                ));
    }

    public ServiceResponse<Boolean> deleteCredentials(String userSub) {
        boolean deleted = tokenRepo.deleteByUserSub(userSub);
        if (deleted) {
            return ServiceResponseDirector.successOk(true, "Credentials deleted");
        }
        return ServiceResponseDirector.errorNotFound(
                "No Binance credentials stored for user: " + userSub
        );
    }

    private String sign(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
            byte[] rawHmac = mac.doFinal(data.getBytes());

            StringBuilder hexString = new StringBuilder(2 * rawHmac.length);
            for (byte b : rawHmac) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error signing Binance request", e);
        }
    }

    public ServiceResponse<FullPortfolioDTO> getPortfolio(String userSub, String currency) {
        currency = currency.toUpperCase();

        var optionalToken = tokenRepo.findByUserSub(userSub);
        if (optionalToken.isEmpty()) {
            return ServiceResponseDirector.errorNotFound("No Binance credentials stored for user.");
        }
        BinanceToken creds = optionalToken.get();

        List<TickerPriceDTO> tickers;
        try {
            tickers = client.get24hrTicker();
        } catch (Exception e) {
            return ServiceResponseDirector.errorInternal("Failed to fetch market data: " + e.getMessage());
        }

        BigDecimal usdtToEur = tickers.stream()
                .filter(t -> t.symbol.equalsIgnoreCase("EURUSDT") ||
                        t.symbol.equalsIgnoreCase("USDTEUR"))
                .findFirst()
                .map(t -> t.symbol.equalsIgnoreCase("EURUSDT")
                        ? BigDecimal.ONE.divide(t.lastPrice, 8, RoundingMode.HALF_UP)
                        : t.lastPrice)
                .orElse(BigDecimal.ONE);

        BigDecimal conversionRate = currency.equals("EUR") ? usdtToEur : BigDecimal.ONE;

        List<TickerPriceDTO> topTickers = tickers.stream()
                .filter(t -> t.symbol.endsWith("USDT"))
                .sorted((a, b) -> b.lastPrice.compareTo(a.lastPrice))
                .limit(10)
                .toList();

        List<TopCoinDTO> topMarketCoins = topTickers.stream()
                .map(t -> new TopCoinDTO(
                        t.symbol.replace("USDT", ""),
                        t.lastPrice.multiply(conversionRate),
                        t.priceChangePercent,
                        t.volume,
                        t.quoteVolume,
                        t.highPrice.multiply(conversionRate),
                        t.lowPrice.multiply(conversionRate)
                ))
                .toList();

        Map<String, BigDecimal> priceMap = tickers.stream()
                .collect(Collectors.toMap(t -> t.symbol, t -> t.lastPrice));

        long ts = System.currentTimeMillis();
        String query = "recvWindow=60000&timestamp=" + ts;
        String sig = sign(query, creds.getSecretKey());

        BinanceAccountDTO account;
        try {
            account = client.getAccount(creds.getApiKey(), 60000, ts, sig);
        } catch (Exception e) {
            return ServiceResponseDirector.errorInternal("Failed to fetch account info: " + e.getMessage());
        }

        List<CoinPortfolioEntryDTO> myCoins = new ArrayList<>();
        BigDecimal totalValue = BigDecimal.ZERO;

        for (TopCoinDTO coin : topMarketCoins) {
            String asset = coin.getSymbol();
            String pair = asset + "USDT";

            BinanceAccountDTO.BalanceDTO bal = account.getBalances().stream()
                    .filter(b -> b.getAsset().equals(asset))
                    .findFirst()
                    .orElse(null);

            BigDecimal free = bal != null ? new BigDecimal(bal.getFree()) : BigDecimal.ZERO;
            BigDecimal locked = bal != null ? new BigDecimal(bal.getLocked()) : BigDecimal.ZERO;
            BigDecimal total = free.add(locked);
            BigDecimal usdtPrice = priceMap.getOrDefault(pair, BigDecimal.ZERO);
            BigDecimal value = usdtPrice.multiply(total).multiply(conversionRate);

            totalValue = totalValue.add(value);

            String tradeQuery = "symbol=" + pair + "&limit=5&recvWindow=60000&timestamp=" + ts;
            String tradeSig = sign(tradeQuery, creds.getSecretKey());

            List<TradeDTO> trades;
            try {
                trades = client.getMyTrades(creds.getApiKey(), pair, 5, 60000, ts, tradeSig);
            } catch (Exception e) {
                trades = List.of();
            }

            myCoins.add(new CoinPortfolioEntryDTO(asset, free, locked, value, trades));
        }

        FullPortfolioDTO dto = new FullPortfolioDTO(topMarketCoins, myCoins, totalValue);
        return ServiceResponseDirector.successOk(dto, "Portfolio fetched successfully.");
    }
}