package scheduler;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.InjectSpy;
import model.entity.PlannedTx;
import model.entity.SavingsGoal;
import org.junit.jupiter.api.Test;
import repository.BudgetRepository;
import repository.CategoryRepository;
import repository.PlannedTxRepository;
import repository.SavingsGoalRepository;
import repository.TransactionRepository;
import service.NotificationService;
import utils.NoDbProfile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(NoDbProfile.class)
class GoalAndReminderNotificationSchedulerTest {

    @InjectSpy
    GoalAndReminderNotificationScheduler sch;

    @InjectMock
    TransactionRepository txRepo;
    @InjectMock
    SavingsGoalRepository goalRepo;
    @InjectMock
    PlannedTxRepository plannedRepo;
    @InjectMock
    CategoryRepository categoryRepo;
    @InjectMock
    NotificationService notifier;
    @InjectMock
    BudgetRepository budgetRepo;

    @Test
    void dailyGoalAndReminderNotifications_WithTwoBudgetUsers_QueriesPlannedTxForEachUser() {
        when(budgetRepo.findAllUsersWithBudgets()).thenReturn(List.of("u1", "u2"));
        when(goalRepo.findByUser(anyString())).thenReturn(null);
        when(plannedRepo.findByUserAndDueBetween(anyString(), any(), any())).thenReturn(List.of());

        sch.dailyGoalAndReminderNotifications();

        verify(plannedRepo, times(2)).findByUserAndDueBetween(anyString(), any(), any());
    }

    @Test
    void dailyGoalAndReminderNotifications_WithSavingsGoalBehindSchedule_SendsGoalRiskNotification() {
        when(budgetRepo.findAllUsersWithBudgets()).thenReturn(List.of("u"));
        var goal = new SavingsGoal();
        goal.setTargetAmt(new BigDecimal("1000"));
        goal.setTargetDate(LocalDate.now().plusDays(10));
        when(goalRepo.findByUser("u")).thenReturn(goal);
        when(txRepo.sumByCategory("u", "Savings")).thenReturn(new BigDecimal("100"));

        sch.dailyGoalAndReminderNotifications();

        verify(notifier).sendToExternalId(eq("u"), contains("Savings Goal"), anyString());
    }

    @Test
    void dailyGoalAndReminderNotifications_WithPlannedTxDueIn1To3Days_SendsUpcomingPaymentNotification() {
        when(budgetRepo.findAllUsersWithBudgets()).thenReturn(List.of("u"));
        when(goalRepo.findByUser("u")).thenReturn(null);

        var p = new PlannedTx();
        p.setUserSub("u");
        p.setCategoryId(7L);
        p.setTitle("Internet bill");
        p.setDueDate(LocalDate.now().plusDays(2));
        when(plannedRepo.findByUserAndDueBetween(eq("u"), any(), any()))
                .thenReturn(List.of(p));
        when(categoryRepo.findCategoryNameById(7L)).thenReturn("Utilities");

        sch.dailyGoalAndReminderNotifications();

        verify(notifier).sendToExternalId(eq("u"), contains("Upcoming Planned Payment"),
                contains("Utilities"));
    }
}
