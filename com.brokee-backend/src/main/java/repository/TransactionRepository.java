package repository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;
import model.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@ApplicationScoped
public class TransactionRepository implements PanacheRepository<Transaction> {

    public Transaction create(Transaction tx) {
        persist(tx);
        return tx;
    }

    public Transaction update(Transaction tx) {
        return getEntityManager().merge(tx);
    }

    public Transaction findById(Long id) {
        return find("id", id).firstResult();
    }

    public BigDecimal getTotalIncome(String userSub) {
        return sumByType(userSub, "I");
    }

    public BigDecimal getTotalExpenses(String userSub) {
        return sumByType(userSub, "E");
    }

    public BigDecimal getBalance(String userSub) {
        return getTotalIncome(userSub).subtract(getTotalExpenses(userSub));
    }

    public List<Transaction> findRecent(String userSub, int limit) {
        return find(
                "userSub",
                Sort.by("txTime").descending(),
                userSub
        )
                .page(Page.ofSize(limit))
                .list();
    }

    public PanacheQuery<Transaction> findByUserSorted(String userSub) {
        return find(
                "userSub",
                Sort.by("txTime").descending(),
                userSub
        );
    }

    public List<Transaction> findByDateRange(String userSub,
                                             LocalDateTime start,
                                             LocalDateTime end) {
        return find(
                "userSub = ?1 and txTime between ?2 and ?3",
                Sort.descending("txTime"),
                userSub, start, end
        ).list();
    }

    public List<DailySum> findDailyExpenses(String userSub,
                                            LocalDate start,
                                            LocalDate end) {
        TypedQuery<Object[]> q = getEntityManager()
                .createQuery(
                        "SELECT CAST(t.txTime AS date), COALESCE(SUM(t.amount),0) " +
                                "FROM Transaction t " +
                                "WHERE t.userSub = :u AND t.type = 'E' " +
                                "  AND t.txTime >= :from AND t.txTime < :to " +
                                "GROUP BY CAST(t.txTime AS date) " +
                                "ORDER BY CAST(t.txTime AS date)",
                        Object[].class
                );
        q.setParameter("u", userSub);
        q.setParameter("from", start.atStartOfDay());
        q.setParameter("to", end.plusDays(1).atStartOfDay());
        return q.getResultList()
                .stream()
                .map(r -> new DailySum((LocalDate) r[0], (BigDecimal) r[1]))
                .toList();
    }

    public List<MonthlySum> findMonthlyExpenses(String userSub, int year) {
        TypedQuery<Object[]> q = getEntityManager()
                .createQuery(
                        "SELECT FUNCTION('MONTH', t.txTime), COALESCE(SUM(t.amount),0) " +
                                "FROM Transaction t " +
                                "WHERE t.userSub = :u AND t.type = 'E' " +
                                "  AND FUNCTION('YEAR', t.txTime) = :yr " +
                                "GROUP BY FUNCTION('MONTH', t.txTime) " +
                                "ORDER BY FUNCTION('MONTH', t.txTime)",
                        Object[].class
                );
        q.setParameter("u", userSub);
        q.setParameter("yr", year);
        return q.getResultList()
                .stream()
                .map(r -> new MonthlySum(((Number) r[0]).intValue(),
                        (BigDecimal) r[1]))
                .toList();
    }

    public Transaction findByIdAndUser(String userSub, Long id) {
        return find(
                "userSub = ?1 and id = ?2",
                userSub,
                id
        ).firstResult();
    }

    public boolean deleteByIdAndUser(String userSub, Long id) {
        return delete(
                "userSub = ?1 and id = ?2",
                userSub,
                id
        ) > 0;
    }

    public List<Transaction> findByUserAndDateRange(String userSub, OffsetDateTime from, OffsetDateTime to) {
        return list(
                "userSub = ?1 and txTime >= ?2 and txTime <= ?3",
                Sort.by("txTime", Sort.Direction.Ascending),
                userSub, from, to
        );
    }

    public BigDecimal sumByType(String userSub, String type) {
        return find("userSub = ?1 and type = ?2", userSub, type)
                .stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public record DailySum(LocalDate day, BigDecimal total) {
    }

    public record MonthlySum(int month, BigDecimal total) {
    }
}
