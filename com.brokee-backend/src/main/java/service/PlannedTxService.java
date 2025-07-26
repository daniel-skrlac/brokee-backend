package service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import mapper.PlannedTxMapper;
import model.entity.PlannedTx;
import model.response.ServiceResponse;
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

    public ServiceResponse<List<PlannedTxResponseDTO>> list(
            String userSub,
            String title,
            LocalDate dueFrom,
            LocalDate dueTo
    ) {
        List<PlannedTx> all;

        boolean hasTitle = title != null && !title.isBlank();
        boolean hasDueFrom = dueFrom != null;
        boolean hasDueTo = dueTo != null;

        if (hasTitle && hasDueFrom && hasDueTo) {
            all = repo.list(
                    "userSub = ?1 and lower(title) like ?2 and dueDate between ?3 and ?4",
                    userSub, "%" + title.toLowerCase() + "%", dueFrom, dueTo
            );
        } else if (hasTitle) {
            all = repo.findByUserAndTitle(userSub, title);
        } else if (hasDueFrom && hasDueTo) {
            all = repo.findByUserAndDueBetween(userSub, dueFrom, dueTo);
        } else {
            all = repo.findByUser(userSub);
        }

        var dtos = all.stream()
                .map(map::entityToResponse)
                .toList();

        return ServiceResponseDirector.successOk(dtos, "OK");
    }

    public ServiceResponse<PlannedTxResponseDTO> getById(String userSub, Long id) {
        var e = repo.find("userSub = ?1 and id = ?2", userSub, id)
                .firstResult();
        if (e == null) {
            return ServiceResponseDirector.errorNotFound("Not found");
        }
        return ServiceResponseDirector.successOk(map.entityToResponse(e), "OK");
    }

    @Transactional
    public ServiceResponse<PlannedTxResponseDTO> create(
            String userSub,
            PlannedTxRequestDTO dto
    ) {
        var e = map.requestToEntity(dto);
        e.setUserSub(userSub);
        repo.persist(e);
        return ServiceResponseDirector.successCreated(map.entityToResponse(e), "Created");
    }

    @Transactional
    public ServiceResponse<PlannedTxResponseDTO> update(
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
    public ServiceResponse<Boolean> delete(String userSub, Long id) {
        boolean ok = repo.deleteByUserAndId(userSub, id);
        if (!ok) {
            return ServiceResponseDirector.errorNotFound("Not found");
        }
        return ServiceResponseDirector.successOk(true, "Deleted");
    }
}
