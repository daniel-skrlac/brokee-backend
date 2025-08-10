package service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.InjectSpy;
import mapper.BudgetMapper;
import model.entity.Budget;
import model.helper.PagedResponseDTO;
import model.home.BudgetRequestDTO;
import model.home.BudgetResponseDTO;
import model.response.ServiceResponseDTO;
import org.junit.jupiter.api.Test;
import repository.BudgetRepository;
import utils.NoDbProfile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(NoDbProfile.class)
class BudgetServiceTest {

    @InjectSpy
    BudgetService svc;

    @InjectMock
    BudgetRepository repo;
    @InjectMock
    BudgetMapper map;

    @Test
    void list_ValidRequest_ReturnsPagedAndMapped() {
        when(repo.countByUser("u")).thenReturn(42L);
        var e1 = new Budget();
        var e2 = new Budget();
        when(repo.findByUserPaged("u", 1, 2)).thenReturn(List.of(e1, e2));
        when(map.entityListToResponseList(List.of(e1, e2)))
                .thenReturn(List.of(
                        new BudgetResponseDTO(1L, new BigDecimal("10.00")),
                        new BudgetResponseDTO(2L, new BigDecimal("20.00"))
                ));

        ServiceResponseDTO<PagedResponseDTO<BudgetResponseDTO>> res = svc.list("u", 1, 2);

        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getData().total()).isEqualTo(42);
        assertThat(res.getData().items()).hasSize(2);
    }

    @Test
    void bulkCreate_EmptyList_ReturnsBadRequest() {
        var res = svc.bulkCreate("u", List.of());
        assertThat(res.isSuccess()).isFalse();
    }

    @Test
    void bulkCreate_DuplicateCategoryIds_ReturnsBadRequest() {
        var reqs = List.of(
                new BudgetRequestDTO(10L, new BigDecimal("1.00")),
                new BudgetRequestDTO(10L, new BigDecimal("2.00"))
        );

        var res = svc.bulkCreate("u", reqs);

        assertThat(res.isSuccess()).isFalse();
    }

    @Test
    void bulkCreate_CategoryAlreadyExists_ReturnsConflict() {
        var reqs = List.of(
                new BudgetRequestDTO(10L, new BigDecimal("1.00")),
                new BudgetRequestDTO(11L, new BigDecimal("2.00"))
        );
        when(repo.findExistingCategoryIds("u", Set.of(10L, 11L))).thenReturn(List.of(11L));

        var res = svc.bulkCreate("u", reqs);

        assertThat(res.isSuccess()).isFalse();
    }

    @Test
    void bulkCreate_ValidList_PersistsAndMaps() {
        var reqs = List.of(
                new BudgetRequestDTO(10L, new BigDecimal("1.00")),
                new BudgetRequestDTO(11L, new BigDecimal("2.00"))
        );
        when(repo.findExistingCategoryIds(eq("u"), any())).thenReturn(List.of());

        var b1 = spy(new Budget());
        var b2 = spy(new Budget());
        doNothing().when(b1).persist();
        doNothing().when(b2).persist();

        when(map.requestToEntity(reqs.get(0))).thenReturn(b1);
        when(map.requestToEntity(reqs.get(1))).thenReturn(b2);
        when(map.entityToResponse(b1)).thenReturn(new BudgetResponseDTO(10L, new BigDecimal("1.00")));
        when(map.entityToResponse(b2)).thenReturn(new BudgetResponseDTO(11L, new BigDecimal("2.00")));

        var res = svc.bulkCreate("u", reqs);

        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getData()).hasSize(2);
        assertThat(b1.getUserSub()).isEqualTo("u");
        assertThat(b2.getUserSub()).isEqualTo("u");
        verify(b1).persist();
        verify(b2).persist();
    }

    @Test
    void bulkPatch_EmptyList_ReturnsBadRequest() {
        var res = svc.bulkPatch("u", List.of());
        assertThat(res.isSuccess()).isFalse();
    }

    @Test
    void bulkPatch_MixesExistingAndMissing_OnlyUpdatesExisting() {
        var r1 = new BudgetRequestDTO(1L, new BigDecimal("5"));
        var r2 = new BudgetRequestDTO(2L, new BigDecimal("7"));

        var b1 = spy(new Budget());
        doNothing().when(b1).persist();

        when(repo.findByUserAndCategory("u", 1L)).thenReturn(b1);
        when(repo.findByUserAndCategory("u", 2L)).thenReturn(null);
        when(map.entityToResponse(b1)).thenReturn(new BudgetResponseDTO(1L, new BigDecimal("5")));

        var res = svc.bulkPatch("u", List.of(r1, r2));

        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getData()).hasSize(1);
        verify(b1).persist();
    }

    @Test
    void deleteBulk_EmptyIds_ReturnsBadRequest() {
        var res = svc.deleteBulk("u", List.of());
        assertThat(res.isSuccess()).isFalse();
    }

    @Test
    void deleteBulk_ValidIds_ReturnsDeletedCount() {
        when(repo.deleteByUserAndCategories("u", List.of(1L, 2L))).thenReturn(1L);

        var res = svc.deleteBulk("u", List.of(1L, 2L));

        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getData()).isEqualTo(1L);
    }
}
