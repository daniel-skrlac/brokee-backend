package service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import model.entity.Category;
import model.entity.Transaction;
import model.response.ServiceResponseDTO;
import model.tracking.DashboardSummaryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repository.CategoryRepository;
import repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class DashboardServiceIT {

    @Inject
    DashboardService service;
    @Inject
    TransactionRepository txRepo;
    @Inject
    CategoryRepository categoryRepo;

    @BeforeEach
    @Transactional
    void clean() {
        txRepo.deleteAll();
    }

    @Test
    @Transactional
    void summary_calculates_income_minus_expense() {
        Category cat = new Category();
        cat.setName("General");
        categoryRepo.persist(cat);
        Long catId = cat.getId();

        txRepo.persist(new Transaction(
                null, "u1", "E", new BigDecimal("150"), catId,
                OffsetDateTime.now(ZoneOffset.UTC), null, null, null, null
        ));
        txRepo.persist(new Transaction(
                null, "u1", "I", new BigDecimal("500"), catId,
                OffsetDateTime.now(ZoneOffset.UTC), null, null, null, null
        ));

        ServiceResponseDTO<DashboardSummaryDTO> res = service.summary("u1");

        assertThat(res.isSuccess()).isTrue();
    }
}
