package service;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import mapper.PlannedTxMapper;
import model.entity.PlannedTx;
import model.helper.PagedResponseDTO;
import model.response.ServiceResponseDTO;
import model.transaction.PlannedTxRequestDTO;
import model.transaction.PlannedTxResponseDTO;
import org.junit.jupiter.api.Test;
import repository.PlannedTxRepository;
import utils.NoDbProfile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(NoDbProfile.class)
class PlannedTxServiceTest {

    @Inject
    PlannedTxService svc;
    @InjectMock
    PlannedTxRepository repo;
    @InjectMock
    PlannedTxMapper map;

    @Test
    void list_WithFilters_MapsEntitiesToDTOs() {
        PanacheQuery<PlannedTx> q = mockPlannedQuery();
        when(repo.findWithFilters(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(q);

        PlannedTx e = new PlannedTx();
        when(q.list()).thenReturn(List.of(e));

        PlannedTxResponseDTO dto = new PlannedTxResponseDTO();
        dto.id = 1L;
        dto.type = "E";
        dto.categoryId = 10L;
        dto.title = "Title";
        dto.amount = new BigDecimal("10.00");
        dto.dueDate = LocalDate.now();
        dto.autoBook = true;

        when(map.entityToResponse(e)).thenReturn(dto);

        ServiceResponseDTO<List<PlannedTxResponseDTO>> res =
                svc.list("u", null, null, null, null, null, null, null);

        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getData()).hasSize(1);
        assertThat(res.getData().get(0).title).isEqualTo("Title");
    }

    @Test
    void page_WithFilters_CountsAndPages() {
        PanacheQuery<PlannedTx> q = mockPlannedQuery();
        when(repo.findWithFilters(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(q);

        when(q.count()).thenReturn(5L);
        when(q.page(any(Page.class))).thenReturn(q);

        PlannedTx e = new PlannedTx();
        when(q.list()).thenReturn(List.of(e));

        PlannedTxResponseDTO dto = new PlannedTxResponseDTO();
        dto.id = 2L;
        dto.type = "I";
        dto.categoryId = 2L;
        dto.title = "X";
        dto.amount = new BigDecimal("15.00");
        dto.dueDate = LocalDate.now();
        dto.autoBook = false;

        when(map.entityToResponse(e)).thenReturn(dto);

        ServiceResponseDTO<PagedResponseDTO<PlannedTxResponseDTO>> res =
                svc.page("u", 0, 10, null, null, null, null, null, null, null);

        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getData().total()).isEqualTo(5);
        assertThat(res.getData().items()).hasSize(1);
        assertThat(res.getData().items().get(0).id).isEqualTo(2L);
    }

    @Test
    void getById_NotExisting_ReturnsNotFound() {
        PanacheQuery<PlannedTx> q = mockPlannedQuery();
        when(repo.find(eq("userSub = ?1 and id = ?2"), any(Object[].class))).thenReturn(q);
        when(q.firstResult()).thenReturn(null);

        var res = svc.getById("u", 9L);

        assertThat(res.isSuccess()).isFalse();
    }

    @Test
    void getById_Existing_MapsToDTO() {
        PlannedTx e = new PlannedTx();

        PanacheQuery<PlannedTx> q = mockPlannedQuery();
        when(repo.find(eq("userSub = ?1 and id = ?2"), any(Object[].class))).thenReturn(q);
        when(q.firstResult()).thenReturn(e);

        PlannedTxResponseDTO dto = new PlannedTxResponseDTO();
        dto.id = 1L;
        dto.type = "E";
        dto.categoryId = 1L;
        dto.title = "T";
        dto.amount = new BigDecimal("1.00");
        dto.dueDate = LocalDate.now();
        dto.autoBook = true;

        when(map.entityToResponse(e)).thenReturn(dto);

        var res = svc.getById("u", 1L);

        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getData().id).isEqualTo(1L);
        assertThat(res.getData().title).isEqualTo("T");
    }

    @Test
    void create_ValidRequest_MapsSetsUserAndPersists() {
        PlannedTxRequestDTO req = new PlannedTxRequestDTO();
        req.type = "E";
        req.categoryId = 3L;
        req.title = "T";
        req.amount = new BigDecimal("10.00");
        req.dueDate = LocalDate.now();
        req.autoBook = true;

        PlannedTx entity = new PlannedTx();
        when(map.requestToEntity(req)).thenReturn(entity);

        PlannedTxResponseDTO dto = new PlannedTxResponseDTO();
        dto.id = 100L;
        dto.type = req.type;
        dto.categoryId = req.categoryId;
        dto.title = req.title;
        dto.amount = req.amount;
        dto.dueDate = req.dueDate;
        dto.autoBook = req.autoBook;

        when(map.entityToResponse(entity)).thenReturn(dto);

        var res = svc.create("u", req);

        assertThat(res.isSuccess()).isTrue();
        assertThat(entity.getUserSub()).isEqualTo("u");
        verify(repo).persist(entity);
        assertThat(res.getData().id).isEqualTo(100L);
    }

    @Test
    void update_NotExisting_ReturnsNotFound() {
        PanacheQuery<PlannedTx> q = mockPlannedQuery();
        when(repo.find(eq("userSub = ?1 and id = ?2"), any(Object[].class))).thenReturn(q);
        when(q.firstResult()).thenReturn(null);

        PlannedTxRequestDTO req = new PlannedTxRequestDTO();
        req.type = "E";
        req.categoryId = 1L;
        req.title = "X";
        req.amount = new BigDecimal("1.00");
        req.dueDate = LocalDate.now();
        req.autoBook = true;

        var res = svc.update("u", 5L, req);

        assertThat(res.isSuccess()).isFalse();
    }

    @Test
    void update_Existing_MapsAndPersists() {
        PlannedTx e = new PlannedTx();
        e.setId(7L);

        PanacheQuery<PlannedTx> q = mockPlannedQuery();
        when(repo.find(eq("userSub = ?1 and id = ?2"), any(Object[].class))).thenReturn(q);
        when(q.firstResult()).thenReturn(e);

        PlannedTxRequestDTO req = new PlannedTxRequestDTO();
        req.type = "E";
        req.categoryId = 9L;
        req.title = "New";
        req.amount = new BigDecimal("2.00");
        req.dueDate = LocalDate.now();
        req.autoBook = false;

        doAnswer(inv -> {
            PlannedTxRequestDTO r = inv.getArgument(0);
            PlannedTx target = inv.getArgument(1);
            target.setTitle(r.title);
            target.setCategoryId(r.categoryId);
            target.setAmount(r.amount);
            target.setType(r.type);
            target.setAutoBook(r.autoBook);
            target.setDueDate(r.dueDate);
            return null;
        }).when(map).updateFromDto(eq(req), eq(e));

        PlannedTxResponseDTO dto = new PlannedTxResponseDTO();
        dto.id = 7L;
        dto.type = "E";
        dto.categoryId = 9L;
        dto.title = "New";
        dto.amount = new BigDecimal("2.00");
        dto.dueDate = req.dueDate;
        dto.autoBook = false;

        when(map.entityToResponse(e)).thenReturn(dto);

        var res = svc.update("u", 7L, req);

        assertThat(res.isSuccess()).isTrue();
        verify(repo).persist(e);
        assertThat(res.getData().title).isEqualTo("New");
        assertThat(res.getData().categoryId).isEqualTo(9L);
    }

    @Test
    void delete_NotDeleted_ReturnsNotFound() {
        when(repo.deleteByUserAndId("u", 1L)).thenReturn(false);
        assertThat(svc.delete("u", 1L).isSuccess()).isFalse();
    }

    @Test
    void delete_Deleted_ReturnsSuccess() {
        when(repo.deleteByUserAndId("u", 2L)).thenReturn(true);
        assertThat(svc.delete("u", 2L).isSuccess()).isTrue();
    }

    @SuppressWarnings("unchecked")
    private PanacheQuery<PlannedTx> mockPlannedQuery() {
        return (PanacheQuery<PlannedTx>) mock(PanacheQuery.class);
    }
}
