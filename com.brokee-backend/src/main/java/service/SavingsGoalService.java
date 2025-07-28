package service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import mapper.SavingsGoalMapper;
import model.response.ServiceResponseDTO;
import model.response.ServiceResponseDirector;
import model.settings.SavingsGoalRequestDTO;
import model.settings.SavingsGoalResponseDTO;
import repository.SavingsGoalRepository;

@ApplicationScoped
public class SavingsGoalService {
    @Inject
    SavingsGoalRepository repo;
    @Inject
    SavingsGoalMapper map;

    public ServiceResponseDTO<SavingsGoalResponseDTO> get(String userSub) {
        var e = repo.findByUser(userSub);
        if (e == null) {
            return ServiceResponseDirector.errorNotFound("No savings goal found");
        }
        return ServiceResponseDirector.successOk(map.entityToResponse(e), "OK");
    }

    @Transactional
    public ServiceResponseDTO<SavingsGoalResponseDTO> upsert(
            String userSub,
            SavingsGoalRequestDTO dto
    ) {
        var existing = repo.findByUser(userSub);
        if (existing == null) {
            existing = map.requestToEntity(dto);
            existing.setUserSub(userSub);
            existing.persist();
            return ServiceResponseDirector.successCreated(
                    map.entityToResponse(existing),
                    "Savings goal created"
            );
        } else {
            existing.setTargetAmt(dto.targetAmt);
            existing.setTargetDate(dto.targetDate);
            existing.persist();
            return ServiceResponseDirector.successOk(
                    map.entityToResponse(existing),
                    "Savings goal updated"
            );
        }
    }

    @Transactional
    public ServiceResponseDTO<SavingsGoalResponseDTO> patch(
            String userSub,
            SavingsGoalRequestDTO dto
    ) {
        var e = repo.findByUser(userSub);
        if (e == null) {
            return ServiceResponseDirector.errorNotFound("No savings goal to patch");
        }
        if (dto.targetAmt != null) e.setTargetAmt(dto.targetAmt);
        if (dto.targetDate != null) e.setTargetDate(dto.targetDate);
        e.persist();
        return ServiceResponseDirector.successOk(
                map.entityToResponse(e),
                "Savings goal partially updated"
        );
    }

    @Transactional
    public ServiceResponseDTO<Boolean> delete(String userSub) {
        boolean ok = repo.deleteByUser(userSub);
        if (!ok) {
            return ServiceResponseDirector.errorNotFound("No savings goal to delete");
        }
        return ServiceResponseDirector.successOk(true, "Deleted");
    }
}
