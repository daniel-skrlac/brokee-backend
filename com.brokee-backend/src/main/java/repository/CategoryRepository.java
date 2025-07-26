package repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import model.entity.Category;

@ApplicationScoped
public class CategoryRepository implements PanacheRepository<Category> {
}
