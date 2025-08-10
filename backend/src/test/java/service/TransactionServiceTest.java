package service;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.persistence.Tuple;
import mapper.TransactionMapper;
import model.entity.Transaction;
import model.helper.PagedResponseDTO;
import model.home.FullTxRequestDTO;
import model.home.TxResponseDTO;
import model.response.ServiceResponseDTO;
import model.tracking.CategoryBreakdownDTO;
import model.tracking.LocationDTO;
import model.tracking.SpendingVsIncomeDTO;
import org.junit.jupiter.api.Test;
import repository.BudgetRepository;
import repository.CategoryRepository;
import repository.TransactionRepository;
import utils.NoDbProfile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(NoDbProfile.class)
class TransactionServiceTest {

    @InjectMock
    NotificationService notifier;

    @InjectMock
    TransactionRepository txRepo;
    @InjectMock
    BudgetRepository budgetRepo;
    @InjectMock
    CategoryRepository categoryRepo;
    @InjectMock
    TransactionMapper mapper;
    @InjectMock
    LocationService locationService;

    @Inject
    TransactionService svc;

    @Test
    void findById_FoundAndMissing_ReturnsSuccessAndNotFound() {
        var t = new Transaction();
        t.setId(5L);
        when(txRepo.findByIdAndUser("u", 5L)).thenReturn(t);
        when(mapper.entityToResponse(t))
                .thenReturn(new TxResponseDTO(5L, "E", new BigDecimal("1"), null, null, null, null));

        assertThat(svc.findById("u", 5L).isSuccess()).isTrue();

        when(txRepo.findByIdAndUser("u", 6L)).thenReturn(null);
        assertThat(svc.findById("u", 6L).isSuccess()).isFalse();
    }

    @Test
    void spendingVsIncome_MapsFromTuples_ReturnsDTOs() {
        Tuple a = mock(Tuple.class);
        when(a.get("month", String.class)).thenReturn("2025-01");
        when(a.get("expenses", BigDecimal.class)).thenReturn(new BigDecimal("10"));
        when(a.get("income", BigDecimal.class)).thenReturn(new BigDecimal("50"));
        when(txRepo.findSpendingVsIncomeByYear("u", 2025)).thenReturn(List.of(a));

        ServiceResponseDTO<List<SpendingVsIncomeDTO>> res = svc.spendingVsIncome("u", 2025);
        assertThat(res.getData()).singleElement().satisfies(it -> {
            assertThat(it.month()).isEqualTo("2025-01");
            assertThat(it.income()).isEqualByComparingTo("50");
        });
    }

    @Test
    void categoryBreakdown_Maps_ReturnsList() {
        Tuple a = mock(Tuple.class);
        when(a.get("category", String.class)).thenReturn("Food");
        when(a.get("amount", BigDecimal.class)).thenReturn(new BigDecimal("12.34"));
        when(txRepo.findCategoryBreakdown("u", "2025-08")).thenReturn(List.of(a));

        ServiceResponseDTO<List<CategoryBreakdownDTO>> res = svc.categoryBreakdown("u", "2025-08");
        assertThat(res.getData()).extracting(CategoryBreakdownDTO::category).containsExactly("Food");
    }

    @Test
    void topLocations_Maps_ReturnsList() {
        Tuple a = mock(Tuple.class);
        when(a.get("latitude", BigDecimal.class)).thenReturn(new BigDecimal("1.1"));
        when(a.get("longitude", BigDecimal.class)).thenReturn(new BigDecimal("2.2"));
        when(a.get("label", String.class)).thenReturn("Place");
        when(a.get("amount", BigDecimal.class)).thenReturn(new BigDecimal("3.3"));
        when(txRepo.findTopLocations("u", 5)).thenReturn(List.of(a));

        ServiceResponseDTO<List<LocationDTO>> res = svc.topLocations("u", 5);
        assertThat(res.getData()).hasSize(1);
        assertThat(res.getData().get(0).label()).isEqualTo("Place");
    }

    @Test
    void recent_Maps_ReturnsDTOs() {
        var t = new Transaction();
        when(txRepo.findRecent("u", 3)).thenReturn(List.of(t));
        when(mapper.entityToResponse(t))
                .thenReturn(new TxResponseDTO(1L, "E", BigDecimal.ONE, null, null, null, null));

        assertThat(svc.recent("u", 3).getData()).hasSize(1);
    }

    @Test
    void page_InvalidDate_ReturnsBadRequest() {
        var bad = svc.page("u", 0, 10, null, null, null, "not-a-date", null, null, null);
        assertThat(bad.isSuccess()).isFalse();
        assertThat(bad.getMessage()).contains("Invalid date");
    }

    @Test
    @SuppressWarnings("unchecked")
    void page_CountsPagesAndMaps_ReturnsPaged() {
        PanacheQuery<Transaction> q = mock(PanacheQuery.class);
        when(txRepo.findByUserWithFilters(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(q);
        when(q.count()).thenReturn(2L);
        when(q.page(any(Page.class))).thenReturn(q);
        var t = new Transaction();
        when(q.list()).thenReturn(List.of(t));
        when(mapper.entityToResponse(t))
                .thenReturn(new TxResponseDTO(7L, "I", BigDecimal.TEN, null, null, null, null));

        ServiceResponseDTO<PagedResponseDTO<TxResponseDTO>> res =
                svc.page("u", 1, 5, null, null, null, null, null, null, null);

        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getData().total()).isEqualTo(2);
        assertThat(res.getData().items()).hasSize(1);
    }

    @Test
    void getBalance_SumsIncomeMinusExpense_ReturnsBalance() {
        when(txRepo.sumByType("u", "I")).thenReturn(new BigDecimal("100"));
        when(txRepo.sumByType("u", "E")).thenReturn(new BigDecimal("30"));
        assertThat(svc.getBalance("u").getData()).isEqualByComparingTo("70");
    }

    @Test
    void findByDateRange_Maps_ReturnsList() {
        var t = new Transaction();
        when(txRepo.findByUserAndDateRange(eq("u"), any(), any())).thenReturn(List.of(t));
        when(mapper.entityToResponse(t))
                .thenReturn(new TxResponseDTO(1L, "E", BigDecimal.ONE, null, null, null, null));

        var res = svc.findByDateRange("u", OffsetDateTime.now().minusDays(1), OffsetDateTime.now());
        assertThat(res.getData()).hasSize(1);
    }

    @Test
    void create_WithLatLon_SetsLocationNameAndChecksNotifications() {
        var tx = spy(new Transaction());
        tx.setType("E");
        tx.setAmount(new BigDecimal("10"));
        tx.setLatitude(new BigDecimal("45.0"));
        tx.setLongitude(new BigDecimal("16.0"));
        doNothing().when(tx).persist();

        when(mapper.fullRequestToEntity(any())).thenReturn(tx);
        when(locationService.getLocationName(45.0, 16.0)).thenReturn("Home");
        doNothing().when(notifier).sendToUser(anyString(), anyString(), anyString());

        var req = new FullTxRequestDTO(
                "E", new BigDecimal("10"), 1L, LocalDateTime.now(), null,
                new BigDecimal("45.0"), new BigDecimal("16.0")
        );
        var res = svc.create("u", req);

        assertThat(res.isSuccess()).isTrue();
        assertThat(tx.getLocationName()).isEqualTo("Home");
    }

    @Test
    void update_NotFound_Returns404() {
        when(txRepo.findByIdAndUser("u", 1L)).thenReturn(null);
        var req = new FullTxRequestDTO("E", BigDecimal.ONE, 1L, LocalDateTime.now(), null, null, null);
        var res = svc.update("u", 1L, req);
        assertThat(res.isSuccess()).isFalse();
    }

    @Test
    void findDailyExpenses_GroupsIncomePositiveExpenseNegative_ReturnsTotals() {
        var t1 = new Transaction();
        t1.setType("I");
        t1.setAmount(new BigDecimal("10"));
        t1.setTxTime(OffsetDateTime.now().minusHours(2));
        var t2 = new Transaction();
        t2.setType("E");
        t2.setAmount(new BigDecimal("5"));
        t2.setTxTime(OffsetDateTime.now().minusHours(1));

        when(txRepo.findByUserAndDateRange(anyString(), any(), any())).thenReturn(List.of(t1, t2));

        ServiceResponseDTO<Map<String, BigDecimal>> res = svc.findDailyExpenses("u", 1);
        var totals = res.getData().values();
        assertThat(totals).containsExactly(new BigDecimal("10").add(new BigDecimal("5").negate()));
    }

    @Test
    void findMonthlyExpenses_GroupsByYearMonth_ReturnsMap() {
        var jan = new Transaction();
        jan.setType("E");
        jan.setAmount(new BigDecimal("3"));
        jan.setTxTime(OffsetDateTime.parse("2025-01-10T00:00Z"));
        var feb = new Transaction();
        feb.setType("I");
        feb.setAmount(new BigDecimal("7"));
        feb.setTxTime(OffsetDateTime.parse("2025-02-10T00:00Z"));

        when(txRepo.findByUserAndDateRange(anyString(), any(), any())).thenReturn(List.of(jan, feb));

        var res = svc.findMonthlyExpenses("u", 2025);
        assertThat(res.getData())
                .containsEntry("2025-01", new BigDecimal("-3"))
                .containsEntry("2025-02", new BigDecimal("7"));
    }
}
