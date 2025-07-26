package mapper;

import model.entity.Budget;
import model.home.BudgetResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi")
public interface BudgetMapper {
    BudgetResponseDTO entityToResponse(Budget b);
}
