package service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import mapper.SavingsGoalMapper;
import model.response.ServiceResponse;
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

    public ServiceResponse<SavingsGoalResponseDTO> get(String userSub) {
        var e = repo.findByUser(userSub);
        if (e == null) {
            return ServiceResponseDirector.errorNotFound("No savings goal found");
        }
        return ServiceResponseDirector.successOk(map.entityToResponse(e), "OK");
    }

    @Transactional
    public ServiceResponse<SavingsGoalResponseDTO> upsert(
            String userSub,
            SavingsGoalRequestDTO dto
    ) {
        var existing = repo.findByUser(userSub);
        if (existing == null) {
            existing = map.requestToEntity(dto);
            existing.userSub = userSub;
            existing.persist();
            return ServiceResponseDirector.successCreated(
                    map.entityToResponse(existing),
                    "Savings goal created"
            );
        } else {
            existing.targetAmt = dto.targetAmt;
            existing.targetDate = dto.targetDate;
            existing.persist();
            return ServiceResponseDirector.successOk(
                    map.entityToResponse(existing),
                    "Savings goal updated"
            );
        }
    }

    @Transactional
    public ServiceResponse<SavingsGoalResponseDTO> patch(
            String userSub,
            SavingsGoalRequestDTO dto
    ) {
        var e = repo.findByUser(userSub);
        if (e == null) {
            return ServiceResponseDirector.errorNotFound("No savings goal to patch");
        }
        if (dto.targetAmt != null) e.targetAmt = dto.targetAmt;
        if (dto.targetDate != null) e.targetDate = dto.targetDate;
        e.persist();
        return ServiceResponseDirector.successOk(
                map.entityToResponse(e),
                "Savings goal partially updated"
        );
    }

    @Transactional
    public ServiceResponse<Boolean> delete(String userSub) {
        boolean ok = repo.deleteByUser(userSub);
        if (!ok) {
            return ServiceResponseDirector.errorNotFound("No savings goal to delete");
        }
        return ServiceResponseDirector.successOk(true, "Deleted");
    }
}
