package service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import mapper.BudgetMapper;
import model.entity.Budget;
import model.helper.PagedResponseDTO;
import model.home.BudgetRequestDTO;
import model.home.BudgetResponseDTO;
import model.response.ServiceResponse;
import model.response.ServiceResponseDirector;
import repository.BudgetRepository;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@ApplicationScoped
public class BudgetService {

    @Inject
    BudgetRepository repo;
    @Inject
    BudgetMapper map;

    public ServiceResponse<PagedResponseDTO<BudgetResponseDTO>> list(
            String userSub, int page, int size
    ) {
        long total = repo.countByUser(userSub);
        var items = repo.findByUserPaged(userSub, page, size);
        var dtos = map.entityListToResponseList(items);

        return ServiceResponseDirector.successOk(
                new PagedResponseDTO<>(dtos, page, size, total),
                "OK"
        );
    }


    @Transactional
    public ServiceResponse<List<BudgetResponseDTO>> bulkCreate(
            String userSub,
            List<BudgetRequestDTO> dtos
    ) {
        if (dtos == null || dtos.isEmpty()) {
            return ServiceResponseDirector.errorBadRequest("No budgets provided");
        }
        List<Budget> saved = dtos.stream()
                .map(dto -> {
                    Budget b = map.requestToEntity(dto);
                    b.setUserSub(userSub);
                    b.persist();
                    return b;
                })
                .toList();

        List<BudgetResponseDTO> resp = saved.stream()
                .map(map::entityToResponse)
                .collect(Collectors.toList());

        return ServiceResponseDirector.successCreated(resp, "Budgets created");
    }

    @Transactional
    public ServiceResponse<List<BudgetResponseDTO>> bulkPatch(
            String userSub,
            List<BudgetRequestDTO> dtos
    ) {
        if (dtos == null || dtos.isEmpty()) {
            return ServiceResponseDirector.errorBadRequest("No budgets provided");
        }
        List<BudgetResponseDTO> updated = dtos.stream()
                .map(dto -> {
                    Budget b = repo.findByUserAndCategory(userSub, dto.categoryId());
                    if (b == null) {
                        return null;
                    }
                    b.setAmount(dto.amount());
                    b.persist();
                    return map.entityToResponse(b);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return ServiceResponseDirector.successOk(updated, "Budgets updated");
    }

    @Transactional
    public ServiceResponse<Long> deleteBulk(String userSub, List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return ServiceResponseDirector.errorBadRequest("No categoryIds provided");
        }
        long deleted = repo.deleteByUserAndCategories(userSub, categoryIds);
        return ServiceResponseDirector.successOk(
                deleted,
                deleted == categoryIds.size()
                        ? "All budgets deleted"
                        : deleted + " of " + categoryIds.size() + " deleted"
        );
    }
}
