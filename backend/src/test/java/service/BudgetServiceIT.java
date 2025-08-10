package service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import model.entity.Budget;
import model.entity.Category;
import model.helper.PagedResponseDTO;
import model.home.BudgetRequestDTO;
import model.home.BudgetResponseDTO;
import model.response.ServiceResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repository.BudgetRepository;
import repository.CategoryRepository;
import repository.TransactionRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class BudgetServiceIT {
    @Inject
    BudgetService service;
    @Inject
    BudgetRepository repo;
    @Inject
    CategoryRepository catRepo;
    @Inject
    TransactionRepository txRepo;

    Long cat1, cat2, cat3, cat5;

    @BeforeEach
    @Transactional
    void cleanAndSeed() {
        txRepo.deleteAll();
        repo.deleteAll();
        catRepo.deleteAll();

        cat1 = mkCat("Cat 1");
        cat2 = mkCat("Cat 2");
        cat3 = mkCat("Cat 3");
        cat5 = mkCat("Cat 5");
    }

    @Transactional
    Long mkCat(String name) {
        var c = new Category();
        c.setName(name);
        catRepo.persist(c);
        return c.getId();
    }

    @Test
    @Transactional
    void bulkCreate_happyPath() {
        var req = List.of(
                new BudgetRequestDTO(cat1, new BigDecimal("100.00")),
                new BudgetRequestDTO(cat2, new BigDecimal("200.00"))
        );
        var res = service.bulkCreate("u1", req);
        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getStatusCode()).isEqualTo(201);
        assertThat(res.getData()).hasSize(2);

        var list = service.list("u1", 0, 10);
        assertThat(list.getData().items()).hasSize(2);
        assertThat(list.getData().total()).isEqualTo(2);
    }

    @Test
    void bulkCreate_duplicateInRequest_badRequest() {
        var req = List.of(
                new BudgetRequestDTO(cat1, new BigDecimal("100")),
                new BudgetRequestDTO(cat1, new BigDecimal("200"))
        );
        var res = service.bulkCreate("u1", req);
        assertThat(res.isSuccess()).isFalse();
        assertThat(res.getStatusCode()).isEqualTo(400);
    }

    @Test
    @Transactional
    void bulkCreate_conflictOnExisting() {
        repo.persist(new Budget("u1", cat5, new BigDecimal("10.00")));
        var res = service.bulkCreate("u1", List.of(
                new BudgetRequestDTO(cat5, new BigDecimal("100"))
        ));
        assertThat(res.isSuccess()).isFalse();
        assertThat(res.getStatusCode()).isEqualTo(409);
    }

    @Test
    @Transactional
    void bulkPatch_updatesOnlyExisting() {
        repo.persist(new Budget("u1", cat1, new BigDecimal("10")));
        var res = service.bulkPatch("u1", List.of(
                new BudgetRequestDTO(cat1, new BigDecimal("99")),
                new BudgetRequestDTO(cat2, new BigDecimal("55"))
        ));
        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getData()).hasSize(1);
        assertThat(res.getData().get(0).amount()).isEqualByComparingTo("99");
    }

    @Test
    @Transactional
    void deleteBulk_deletesSomeAndCounts() {
        repo.persist(new Budget("u1", cat1, new BigDecimal("10")));
        repo.persist(new Budget("u1", cat2, new BigDecimal("10")));
        var res = service.deleteBulk("u1", List.of(cat1, cat2, cat3));
        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getData()).isEqualTo(2L);
        assertThat(res.getMessage()).contains("2 of 3");
    }

    @Test
    @Transactional
    void list_pagination() {
        for (Long cid : List.of(cat1, cat2, cat3)) {
            repo.persist(new Budget("u1", cid, new BigDecimal("10")));
        }
        ServiceResponseDTO<PagedResponseDTO<BudgetResponseDTO>> p1 = service.list("u1", 0, 2);
        ServiceResponseDTO<PagedResponseDTO<BudgetResponseDTO>> p2 = service.list("u1", 1, 2);

        assertThat(p1.getData().total()).isEqualTo(3);
        assertThat(p1.getData().items()).hasSize(2);
        assertThat(p2.getData().items()).hasSize(1);
    }
}
