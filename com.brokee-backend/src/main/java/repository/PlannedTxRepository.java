package repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import model.entity.PlannedTx;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class PlannedTxRepository implements PanacheRepository<PlannedTx> {
    public List<PlannedTx> findByUser(String userSub) {
        return list("userSub", userSub);
    }

    public List<PlannedTx> findByUserAndTitle(String userSub, String title) {
        return list("userSub = ?1 and title like ?2", userSub, "%" + title + "%");
    }

    public List<PlannedTx> findByUserAndDueBetween(String userSub, LocalDate from, LocalDate to) {
        return list("userSub = ?1 and dueDate between ?2 and ?3", userSub, from, to);
    }

    public List<PlannedTx> findDue(LocalDate date) {
        return list("dueDate = ?1 and autoBook = true", date);
    }

    public boolean deleteByUserAndId(String userSub, Long id) {
        return delete("userSub = ?1 and id = ?2", userSub, id) > 0;
    }
}
