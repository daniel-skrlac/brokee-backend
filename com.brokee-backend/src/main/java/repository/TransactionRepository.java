package repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import model.entity.Transaction;

import java.time.OffsetDateTime;
import java.util.List;

@ApplicationScoped
public class TransactionRepository implements PanacheRepository<Transaction> {
    public List<Transaction> findByUser(String userSub) {
        return list("userSub", userSub);
    }

    public List<Transaction> findByUserAndDateRange(String userSub, OffsetDateTime from, OffsetDateTime to) {
        return list("userSub = ?1 and txTime between ?2 and ?3", userSub, from, to);
    }
}
