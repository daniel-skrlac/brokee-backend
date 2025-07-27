package mapper;

import model.entity.Transaction;
import model.home.FullTxRequestDTO;
import model.home.QuickTxRequestDTO;
import model.home.TxResponseDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "cdi",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface TransactionMapper {

    TxResponseDTO entityToResponse(Transaction t);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userSub", ignore = true)
    @Mapping(target = "note", ignore = true)
    @Mapping(target = "locationName", ignore = true)
    @Mapping(target = "latitude", ignore = true)
    @Mapping(target = "longitude", ignore = true)
    Transaction quickRequestToEntity(QuickTxRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userSub", ignore = true)
    @Mapping(target = "locationName", ignore = true)
    Transaction fullRequestToEntity(FullTxRequestDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userSub", ignore = true)
    @Mapping(target = "note", ignore = true)
    @Mapping(target = "locationName", ignore = true)
    @Mapping(target = "latitude", ignore = true)
    @Mapping(target = "longitude", ignore = true)
    void updateFromFullDto(FullTxRequestDTO dto, @MappingTarget Transaction entity);
}
