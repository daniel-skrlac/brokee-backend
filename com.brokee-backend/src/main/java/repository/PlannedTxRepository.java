package repository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import model.entity.PlannedTx;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class PlannedTxRepository implements PanacheRepository<PlannedTx> {

    public PanacheQuery<PlannedTx> findWithFilters(
            String userSub,
            String title,
            LocalDate dueFrom,
            LocalDate dueTo,
            String type,
            Double minAmount,
            Double maxAmount,
            String categoryName
    ) {
        var ql = new StringBuilder("userSub = ?1");
        var params = new ArrayList<Object>();
        params.add(userSub);
        int idx = 2;

        if (title != null && !title.isBlank()) {
            ql.append(" AND lower(title) LIKE ?").append(idx++);
            params.add("%" + title.toLowerCase() + "%");
        }
        if (dueFrom != null) {
            ql.append(" AND dueDate >= ?").append(idx++);
            params.add(dueFrom);
        }
        if (dueTo != null) {
            ql.append(" AND dueDate <= ?").append(idx++);
            params.add(dueTo);
        }
        if (type != null && !type.isBlank()) {
            ql.append(" AND type = ?").append(idx++);
            params.add(type);
        }
        if (minAmount != null) {
            ql.append(" AND amount >= ?").append(idx++);
            params.add(minAmount);
        }
        if (maxAmount != null) {
            ql.append(" AND amount <= ?").append(idx++);
            params.add(maxAmount);
        }
        if (categoryName != null && !categoryName.isBlank()) {
            ql.append(" AND categoryId IN (")
                    .append(" SELECT c.id FROM Category c ")
                    .append(" WHERE lower(c.name) LIKE ?")
                    .append(idx)
                    .append(")");
            params.add("%" + categoryName.toLowerCase() + "%");
        }

        return find(
                ql.toString(),
                Sort.by("dueDate", Sort.Direction.Descending),
                params.toArray()
        );
    }

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
