package mapper;

import model.entity.PlannedTx;
import model.home.PlanRequestDTO;
import model.home.PlanResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi")
public interface PlannedTxMapper {
    PlanResponseDTO entityToResponse(PlannedTx p);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "autoBook", ignore = true)
    PlannedTx requestToEntity(PlanRequestDTO dto);
}
