package scheduler;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import repository.BinanceTokenRepository;
import repository.CategoryRepository;
import repository.TransactionRepository;
import service.BinanceService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class BinancePortfolioScheduler {

    private static final String SNAPSHOT_NOTE = "Binance Portfolio Snapshot";
    private static final BigDecimal VALUE_CHANGE_THRESHOLD = new BigDecimal("1.00");

    @Inject
    BinanceTokenRepository tokenRepo;

    @Inject
    BinanceService binanceService;

    @Inject
    CategoryRepository categoryRepo;

    @Inject
    TransactionRepository txRepo;

    @Scheduled(cron = "0 59 23 * * ?")
    public void snapshotBinancePortfolio() {
        List<String> users = tokenRepo.findAllUsersWithBinanceTokens();

        Long categoryId = categoryRepo.findIdByName("Investments");
        for (String userSub : users) {
            var response = binanceService.getPortfolio(userSub, "EUR");

            if (response.isSuccess() && response.getData() != null) {
                BigDecimal currentValue = response.getData().getTotalEurValue();
                BigDecimal lastValue = txRepo.getLastSnapshotAmount(userSub, categoryId, SNAPSHOT_NOTE);

                boolean hasChanged = lastValue == null
                        || currentValue.subtract(lastValue).abs().compareTo(VALUE_CHANGE_THRESHOLD) > 0;

                if (!txRepo.snapshotExists(userSub, categoryId, SNAPSHOT_NOTE)) {
                    txRepo.insertInvestmentSnapshot(userSub, currentValue, categoryId, LocalDateTime.now(), SNAPSHOT_NOTE);
                } else if (hasChanged) {
                    txRepo.updateInvestmentSnapshot(userSub, categoryId, SNAPSHOT_NOTE, currentValue, LocalDateTime.now());
                }
            }
        }
    }
}
