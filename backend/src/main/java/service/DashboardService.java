package service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.Tuple;
import model.response.ServiceResponseDTO;
import model.response.ServiceResponseDirector;
import model.tracking.DashboardSummaryDTO;
import repository.TransactionRepository;

import java.math.BigDecimal;

@ApplicationScoped
public class DashboardService {
    @Inject
    TransactionRepository txRepo;

    public ServiceResponseDTO<DashboardSummaryDTO> summary(String userSub) {
        Tuple sums = txRepo.findTotalExpensesAndIncome(userSub);
        BigDecimal expenses = sums.get("totalExpenses", BigDecimal.class);
        BigDecimal income = sums.get("totalIncome", BigDecimal.class);
        DashboardSummaryDTO dto = new DashboardSummaryDTO(
                expenses != null ? expenses : BigDecimal.ZERO,
                income != null ? income : BigDecimal.ZERO,
                (income != null ? income : BigDecimal.ZERO)
                        .subtract(expenses != null ? expenses : BigDecimal.ZERO)
        );
        return ServiceResponseDirector.successOk(dto, "OK");
    }
}
