package scheduler;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import model.entity.PlannedTx;
import model.entity.Transaction;
import repository.PlannedTxRepository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@ApplicationScoped
public class PlannedTxScheduler {

    @Inject
    PlannedTxRepository plannedRepo;

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void bookDuePlannedTransactions() {
        LocalDate today = LocalDate.now();
        List<PlannedTx> due = plannedRepo.findDue(today);
        for (PlannedTx p : due) {
            Transaction t = new Transaction();
            t.setUserSub(p.getUserSub());
            t.setType(p.getType());
            t.setAmount(p.getAmount());
            t.setCategoryId(p.getCategoryId());
            t.setTxTime(OffsetDateTime.now());
            t.persist();
        }
    }
}
