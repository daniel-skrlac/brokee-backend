package mapper;

import model.entity.PlannedTx;
import model.transaction.PlannedTxRequestDTO;
import model.transaction.PlannedTxResponseDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "cdi",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface PlannedTxMapper {
    PlannedTxResponseDTO entityToResponse(PlannedTx e);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userSub", ignore = true)
    PlannedTx requestToEntity(PlannedTxRequestDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromDto(PlannedTxRequestDTO dto, @MappingTarget PlannedTx entity);
}