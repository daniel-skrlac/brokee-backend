package service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import model.entity.PlannedTx;
import model.helper.PagedResponseDTO;
import model.response.ServiceResponseDTO;
import model.transaction.PlannedTxRequestDTO;
import model.transaction.PlannedTxResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repository.PlannedTxRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class PlannedTxServiceIT {

    @Inject
    PlannedTxService svc;
    @Inject
    PlannedTxRepository repo;

    @BeforeEach
    @Transactional
    void clean() {
        repo.deleteAll();
    }

    @Test
    @Transactional
    void create_get_update_delete_flow() {
        var req = new PlannedTxRequestDTO();
        req.title = "Rent";
        req.type = "E";
        req.amount = new BigDecimal("800");
        req.dueDate = LocalDate.now().plusDays(3);
        req.categoryId = 1L;

        var created = svc.create("u1", req);
        assertThat(created.isSuccess()).isTrue();
        Long id = created.getData().id;

        var fetched = svc.getById("u1", id);
        assertThat(fetched.isSuccess()).isTrue();
        assertThat(fetched.getData().title).isEqualTo("Rent");

        var updReq = new PlannedTxRequestDTO();
        updReq.title = "Rent";
        updReq.type = "E";
        updReq.amount = new BigDecimal("820");
        updReq.dueDate = LocalDate.now().plusDays(5);
        updReq.categoryId = 1L;

        var updated = svc.update("u1", id, updReq);
        assertThat(updated.getData().amount).isEqualByComparingTo("820");

        var deleted = svc.delete("u1", id);
        assertThat(deleted.isSuccess()).isTrue();
    }

    @Test
    @Transactional
    void list_and_page_with_filters() {
        repo.persist(new PlannedTx(null, 2L, "u1", "Phone", "E", new BigDecimal("20"), LocalDate.now()));
        repo.persist(new PlannedTx(null, 3L, "u1", "Salary", "I", new BigDecimal("2000"), LocalDate.now()));

        var list = svc.list("u1", null, null, null, null, null, null, null);
        assertThat(list.getData()).hasSize(2);

        ServiceResponseDTO<PagedResponseDTO<PlannedTxResponseDTO>> page =
                svc.page("u1", 0, 1, "Sa", null, null, "I", null, null, null);

        assertThat(page.getData().items()).hasSize(1);
        assertThat(page.getData().total()).isEqualTo(1L);
    }
}
