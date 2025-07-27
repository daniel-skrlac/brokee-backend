package repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import model.entity.Budget;

import java.math.BigDecimal;
import java.util.List;

@ApplicationScoped
public class BudgetRepository implements PanacheRepository<Budget> {

    @Inject
    EntityManager em;

    public List<Budget> findByUserAndMonth(String userSub, String monthKey) {
        return list("userSub = ?1 and monthKey = ?2", userSub, monthKey);
    }

    public long deleteByUserAndCategories(String userSub, List<Long> categoryIds) {
        return delete("userSub = ?1 and categoryId in (?2)", userSub, categoryIds);
    }

    public List<Budget> findByUserPaged(String userSub, int page, int size) {
        return find("userSub", userSub)
                .page(Page.of(page, size))
                .list();
    }

    public long countByUser(String userSub) {
        return count("userSub", userSub);
    }

    public Budget findByUserAndCategory(String userSub, Long categoryId) {
        return find("userSub = ?1 and categoryId = ?2", userSub, categoryId)
                .firstResult();
    }

    public List<String> findAllUsersWithBudgets() {
        return em.createQuery("SELECT DISTINCT b.userSub FROM Budget b", String.class)
                .getResultList();
    }


    public List<Budget> findBudgetsByUser(String userSub) {
        return list("userSub", userSub);
    }

    public BigDecimal findAmountByUserAndCategory(String userSub, Long categoryId) {
        return find("userSub = ?1 and categoryId = ?2", userSub, categoryId)
                .firstResultOptional()
                .map(Budget::getAmount)
                .orElse(null);
    }

}
