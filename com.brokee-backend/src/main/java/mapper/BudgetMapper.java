package mapper;

import model.entity.Budget;
import model.home.BudgetRequestDTO;
import model.home.BudgetResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "cdi")
public interface BudgetMapper {
    BudgetResponseDTO entityToResponse(Budget b);

    List<BudgetResponseDTO> entityListToResponseList(List<Budget> bs);

    @Mapping(target = "userSub", ignore = true)
    @Mapping(target = "amount", source = "dto.amount")
    @Mapping(target = "categoryId", source = "dto.categoryId")
    Budget requestToEntity(BudgetRequestDTO dto);
}
