package repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import model.entity.Budget;

import java.util.List;

@ApplicationScoped
public class BudgetRepository implements PanacheRepository<Budget> {
    public List<Budget> findForUserAndMonth(String userSub, String monthKey) {
        return list("userSub = ?1 and monthKey = ?2", userSub, monthKey);
    }
}
