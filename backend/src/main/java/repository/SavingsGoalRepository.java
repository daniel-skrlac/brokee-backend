package repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import model.entity.SavingsGoal;

@ApplicationScoped
public class SavingsGoalRepository implements PanacheRepository<SavingsGoal> {
    public SavingsGoal findByUser(String userSub) {
        return find("userSub", userSub).firstResult();
    }

    public boolean deleteByUser(String userSub) {
        return delete("userSub", userSub) > 0;
    }
}
