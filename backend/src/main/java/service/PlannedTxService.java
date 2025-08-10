package service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import mapper.PlannedTxMapper;
import model.entity.PlannedTx;
import model.helper.PagedResponseDTO;
import model.response.ServiceResponseDTO;
import model.response.ServiceResponseDirector;
import model.transaction.PlannedTxRequestDTO;
import model.transaction.PlannedTxResponseDTO;
import repository.PlannedTxRepository;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class PlannedTxService {

    @Inject
    PlannedTxRepository repo;
    @Inject
    PlannedTxMapper map;

    public ServiceResponseDTO<List<PlannedTxResponseDTO>> list(
            String userSub,
            String title,
            LocalDate dueFrom,
            LocalDate dueTo,
            String type,
            Double minAmount,
            Double maxAmount,
            String categoryName
    ) {
        List<PlannedTx> all = repo.findWithFilters(
                userSub, title, dueFrom, dueTo,
                type, minAmount, maxAmount,
                categoryName
        ).list();

        var dtos = all.stream()
                .map(map::entityToResponse)
                .toList();

        return ServiceResponseDirector.successOk(dtos, "OK");
    }

    public ServiceResponseDTO<PagedResponseDTO<PlannedTxResponseDTO>> page(
            String userSub,
            int page,
            int size,
            String title,
            LocalDate dueFrom,
            LocalDate dueTo,
            String type,
            Double minAmount,
            Double maxAmount,
            String categoryName
    ) {
        var query = repo.findWithFilters(
                userSub, title, dueFrom, dueTo,
                type, minAmount, maxAmount,
                categoryName
        );

        long total = query.count();
        var items = query.page(io.quarkus.panache.common.Page.of(page, size))
                .list()
                .stream()
                .map(map::entityToResponse)
                .toList();

        var paged = new PagedResponseDTO<>(items, page, size, total);
        return ServiceResponseDirector.successOk(paged, "OK");
    }

    public ServiceResponseDTO<PlannedTxResponseDTO> getById(String userSub, Long id) {
        var e = repo.find("userSub = ?1 and id = ?2", userSub, id)
                .firstResult();
        if (e == null) {
            return ServiceResponseDirector.errorNotFound("Not found");
        }
        return ServiceResponseDirector.successOk(map.entityToResponse(e), "OK");
    }

    @Transactional
    public ServiceResponseDTO<PlannedTxResponseDTO> create(
            String userSub,
            PlannedTxRequestDTO dto
    ) {
        var e = map.requestToEntity(dto);
        e.setUserSub(userSub);
        repo.persist(e);
        return ServiceResponseDirector.successCreated(map.entityToResponse(e), "Created");
    }

    @Transactional
    public ServiceResponseDTO<PlannedTxResponseDTO> update(
            String userSub,
            Long id,
            PlannedTxRequestDTO dto
    ) {
        var e = repo.find("userSub = ?1 and id = ?2", userSub, id)
                .firstResult();
        if (e == null) {
            return ServiceResponseDirector.errorNotFound("Not found");
        }
        map.updateFromDto(dto, e);
        repo.persist(e);
        return ServiceResponseDirector.successOk(map.entityToResponse(e), "Updated");
    }

    @Transactional
    public ServiceResponseDTO<Boolean> delete(String userSub, Long id) {
        boolean ok = repo.deleteByUserAndId(userSub, id);
        if (!ok) {
            return ServiceResponseDirector.errorNotFound("Not found");
        }
        return ServiceResponseDirector.successOk(true, "Deleted");
    }
}
