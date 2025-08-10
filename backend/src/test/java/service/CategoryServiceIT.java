package service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import model.entity.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repository.CategoryRepository;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class CategoryServiceIT {

    @Inject
    CategoryService service;
    @Inject
    CategoryRepository repo;

    @BeforeEach
    @Transactional
    void clean() {
        repo.deleteAll();
    }

    @Test
    @Transactional
    void listAll_and_search() {
        repo.persist(new Category(null, "Groceries"));
        repo.persist(new Category(null, "Gas"));
        var all = service.listAll();
        assertThat(all.isSuccess()).isTrue();
        assertThat(all.getData()).hasSize(2);

        var search = service.search("rocer");
        assertThat(search.getData()).extracting("name").containsExactly("Groceries");
    }

    @Test
    @Transactional
    void getOrCreateRevolutCategory_returnsExistingOrNull() {
        repo.persist(new Category(null, "Revolut"));
        var cat = service.getOrCreateRevolutCategory();
        assertThat(cat).isNotNull();
        assertThat(cat.getName()).isEqualTo("Revolut");
    }
}
