package repository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import model.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@ApplicationScoped
public class TransactionRepository implements PanacheRepository<Transaction> {

    @Inject
    EntityManager em;

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

    public List<Tuple> findSpendingVsIncomeByYear(String userSub, int year) {
        return getEntityManager().createQuery(
                        """
                                SELECT 
                                  FUNCTION('FORMAT', t.txTime, 'yyyy-MM') AS month,
                                  SUM(CASE WHEN t.type = 'E' THEN t.amount ELSE 0 END) AS expenses,
                                  SUM(CASE WHEN t.type = 'I' THEN t.amount ELSE 0 END) AS income
                                FROM Transaction t
                                WHERE t.userSub = :user AND FUNCTION('YEAR', t.txTime) = :yr
                                GROUP BY FUNCTION('FORMAT', t.txTime, 'yyyy-MM')
                                ORDER BY month
                                """, Tuple.class)
                .setParameter("user", userSub)
                .setParameter("yr", year)
                .getResultList();
    }

    public List<Tuple> findCategoryBreakdown(String userSub, String monthKey) {
        return getEntityManager().createQuery(
                        """
                                SELECT 
                                  c.name AS category,
                                  SUM(t.amount) AS amount
                                FROM Transaction t
                                JOIN Category c ON t.categoryId = c.id
                                WHERE t.userSub = :user
                                  AND FUNCTION('FORMAT', t.txTime, 'yyyy-MM') = :mk
                                  AND t.type = 'E'
                                GROUP BY c.name
                                ORDER BY amount DESC
                                """, Tuple.class)
                .setParameter("user", userSub)
                .setParameter("mk", monthKey)
                .getResultList();
    }

    public List<Tuple> findTopLocations(String userSub, int limit) {
        return getEntityManager().createQuery(
                        """
                                SELECT 
                                  t.latitude       AS latitude,
                                  t.longitude      AS longitude,
                                  t.locationName   AS label,
                                  SUM(t.amount)    AS amount
                                FROM Transaction t
                                WHERE t.userSub = :user
                                  AND t.type = 'E'
                                GROUP BY t.latitude, t.longitude, t.locationName
                                ORDER BY SUM(t.amount) DESC
                                """, Tuple.class)
                .setParameter("user", userSub)
                .setMaxResults(limit)
                .getResultList();
    }

    public Tuple findTotalExpensesAndIncome(String userSub) {
        return getEntityManager().createQuery(
                        """
                                SELECT
                                  SUM(CASE WHEN t.type = 'E' THEN t.amount ELSE 0 END) AS totalExpenses,
                                  SUM(CASE WHEN t.type = 'I' THEN t.amount ELSE 0 END) AS totalIncome
                                FROM Transaction t
                                WHERE t.userSub = :user
                                """, Tuple.class)
                .setParameter("user", userSub)
                .getSingleResult();
    }

    public BigDecimal getLastSnapshotAmount(String userSub, Long categoryId, String note) {
        return em.createQuery("""
                        SELECT t.amount FROM tx t
                        WHERE t.userSub = :userSub
                          AND t.category.id = :categoryId
                          AND t.note = :note
                        ORDER BY t.txTime DESC
                        """, BigDecimal.class)
                .setParameter("userSub", userSub)
                .setParameter("categoryId", categoryId)
                .setParameter("note", note)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    public boolean snapshotExists(String userSub, Long categoryId, String note) {
        return em.createQuery("""
                        SELECT COUNT(t) FROM tx t
                        WHERE t.userSub = :userSub AND t.category.id = :categoryId AND t.note = :note
                        """, Long.class)
                .setParameter("userSub", userSub)
                .setParameter("categoryId", categoryId)
                .setParameter("note", note)
                .getSingleResult() > 0;
    }

    public void insertInvestmentSnapshot(String userSub, BigDecimal amount, Long categoryId, LocalDateTime timestamp, String note) {
        em.createNativeQuery("""
                        INSERT INTO tx (user_sub, type, amount, category_id, tx_time, note)
                        VALUES (:userSub, 'I', :amount, :categoryId, :timestamp, :note)
                        """)
                .setParameter("userSub", userSub)
                .setParameter("amount", amount)
                .setParameter("categoryId", categoryId)
                .setParameter("timestamp", timestamp)
                .setParameter("note", note)
                .executeUpdate();
    }

    public void updateInvestmentSnapshot(String userSub, Long categoryId, String note, BigDecimal newAmount, LocalDateTime newTime) {
        em.createQuery("""
                        UPDATE tx t SET t.amount = :newAmount, t.txTime = :newTime
                        WHERE t.userSub = :userSub AND t.category.id = :categoryId AND t.note = :note
                        """)
                .setParameter("newAmount", newAmount)
                .setParameter("newTime", newTime)
                .setParameter("userSub", userSub)
                .setParameter("categoryId", categoryId)
                .setParameter("note", note)
                .executeUpdate();
    }

    public record DailySum(LocalDate day, BigDecimal total) {
    }

    public record MonthlySum(int month, BigDecimal total) {
    }
}
