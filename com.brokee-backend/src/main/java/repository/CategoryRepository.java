package repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import model.entity.Category;

import java.util.List;

@ApplicationScoped
public class CategoryRepository implements PanacheRepository<Category> {
    public List<Category> findByNameLike(String term) {
        String pattern = "%" + term.toLowerCase() + "%";
        return list("lower(name) like ?1", pattern);
    }
}
