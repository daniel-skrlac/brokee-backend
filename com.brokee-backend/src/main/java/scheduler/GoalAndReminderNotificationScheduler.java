package scheduler;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import model.entity.PlannedTx;
import repository.BudgetRepository;
import repository.CategoryRepository;
import repository.PlannedTxRepository;
import repository.SavingsGoalRepository;
import repository.TransactionRepository;
import service.NotificationService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class GoalAndReminderNotificationScheduler {

    private static final BigDecimal SAVINGS_DAILY_REQUIRED_THRESHOLD = new BigDecimal("10.00");

    @Inject
    TransactionRepository txRepo;
    @Inject
    SavingsGoalRepository goalRepo;
    @Inject
    PlannedTxRepository plannedTxRepo;
    @Inject
    CategoryRepository categoryRepo;
    @Inject
    NotificationService notifier;

    @Inject
    BudgetRepository budgetRepo;

    @Scheduled(cron = "0 7 * * *")
    public void dailyGoalAndReminderNotifications() {
        List<String> users = budgetRepo.findAllUsersWithBudgets();

        for (String userSub : users) {
            checkSavingsGoalRisk(userSub);
            checkUpcomingPlannedTx(userSub);
        }
    }

    private void checkSavingsGoalRisk(String userSub) {
        var goal = goalRepo.findByUser(userSub);
        if (goal == null) return;

        BigDecimal currentSavings = txRepo.sumByCategory(userSub, "Savings");
        if (currentSavings == null) currentSavings = BigDecimal.ZERO;

        long daysLeft = LocalDate.now().until(goal.getTargetDate()).getDays();
        if (daysLeft <= 0) return;

        BigDecimal requiredDaily = goal.getTargetAmt()
                .subtract(currentSavings)
                .divide(BigDecimal.valueOf(daysLeft), 2, RoundingMode.HALF_UP);

        if (requiredDaily.compareTo(SAVINGS_DAILY_REQUIRED_THRESHOLD) > 0) {
            notifier.sendToUser(
                    userSub,
                    "🎯 Savings Goal at Risk",
                    "You're falling behind on your savings goal."
            );
        }
    }

    private void checkUpcomingPlannedTx(String userSub) {
        LocalDate from = LocalDate.now().plusDays(1);
        LocalDate to = LocalDate.now().plusDays(3);

        List<PlannedTx> upcoming = plannedTxRepo.findByUserAndDueBetween(userSub, from, to);

        for (PlannedTx tx : upcoming) {
            String catName = categoryRepo.findCategoryNameById(tx.getCategoryId());
            notifier.sendToUser(
                    userSub,
                    "🕒 Upcoming Planned Payment",
                    "Your planned payment for " + catName + " (" + tx.getTitle() + ") is due on " + tx.getDueDate() + "."
            );
        }
    }
}
