package mapper;

import model.entity.SavingsGoal;
import model.settings.SavingsGoalRequestDTO;
import model.settings.SavingsGoalResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi")
public interface SavingsGoalMapper {
    SavingsGoalResponseDTO entityToResponse(SavingsGoal e);

    @Mapping(target = "userSub", ignore = true)
    SavingsGoal requestToEntity(SavingsGoalRequestDTO dto);
}
