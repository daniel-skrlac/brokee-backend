package repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import model.entity.PlannedTx;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class PlannedTxRepository implements PanacheRepository<PlannedTx> {
    public List<PlannedTx> findDueUpTo(String userSub, LocalDate upTo) {
        return list("userSub = ?1 and dueDate <= ?2", userSub, upTo);
    }
}
