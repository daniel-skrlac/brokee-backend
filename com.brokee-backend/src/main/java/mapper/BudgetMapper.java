package mapper;

import model.entity.Budget;
import model.home.BudgetRequestDTO;
import model.home.BudgetResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "cdi",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface BudgetMapper {

    BudgetResponseDTO entityToResponse(Budget b);

    List<BudgetResponseDTO> entityListToResponseList(List<Budget> bs);

    @Mapping(target = "userSub", ignore = true)
    @Mapping(target = "categoryId", source = "categoryId")
    @Mapping(target = "amount", source = "amount")
    Budget requestToEntity(BudgetRequestDTO dto);
}
