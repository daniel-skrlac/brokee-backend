package repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import model.entity.Category;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CategoryRepository implements PanacheRepository<Category> {
    @Inject
    EntityManager em;

    public List<Category> findByNameLike(String term) {
        String pattern = "%" + term.toLowerCase() + "%";
        return list("lower(name) like ?1", pattern);
    }

    public Optional<Category> findByName(String name) {
        return find("name", name).firstResultOptional();
    }

    public Long findIdByName(String name) {
        return em.createQuery("SELECT c.id FROM Category c WHERE c.name = :name", Long.class)
                .setParameter("name", name)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }
}
