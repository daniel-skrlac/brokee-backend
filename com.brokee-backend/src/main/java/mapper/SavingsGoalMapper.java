package mapper;

import model.entity.SavingsGoal;
import model.settings.SavingsGoalRequestDTO;
import model.settings.SavingsGoalResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "cdi",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface SavingsGoalMapper {
    SavingsGoalResponseDTO entityToResponse(SavingsGoal e);

    @Mapping(target = "userSub", ignore = true)
    SavingsGoal requestToEntity(SavingsGoalRequestDTO dto);
}
