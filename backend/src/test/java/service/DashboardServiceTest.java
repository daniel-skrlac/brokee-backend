package service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.persistence.Tuple;
import model.response.ServiceResponseDTO;
import model.tracking.DashboardSummaryDTO;
import org.junit.jupiter.api.Test;
import repository.TransactionRepository;
import utils.NoDbProfile;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(NoDbProfile.class)
class DashboardServiceTest {

    @InjectMock
    TransactionRepository txRepo;
    @Inject
    DashboardService svc;

    @Test
    void summary_NullsFromRepository_ReturnsZeros() {
        Tuple t = mock(Tuple.class);
        when(t.get("totalExpenses", BigDecimal.class)).thenReturn(null);
        when(t.get("totalIncome", BigDecimal.class)).thenReturn(null);
        when(txRepo.findTotalExpensesAndIncome("u")).thenReturn(t);

        ServiceResponseDTO<DashboardSummaryDTO> res = svc.summary("u");

        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getData().totalExpenses()).isZero();
        assertThat(res.getData().totalIncome()).isZero();
        assertThat(res.getData().budgetRemaining()).isZero();
    }

    @Test
    void summary_RepositoryReturnsNumbers_ComputesNet() {
        Tuple t = mock(Tuple.class);
        when(t.get("totalExpenses", BigDecimal.class)).thenReturn(new BigDecimal("25"));
        when(t.get("totalIncome", BigDecimal.class)).thenReturn(new BigDecimal("100"));
        when(txRepo.findTotalExpensesAndIncome("u")).thenReturn(t);

        var res = svc.summary("u");

        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getData().totalExpenses()).isEqualByComparingTo("25");
        assertThat(res.getData().totalIncome()).isEqualByComparingTo("100");
        assertThat(res.getData().budgetRemaining()).isEqualByComparingTo("75");
    }
}
