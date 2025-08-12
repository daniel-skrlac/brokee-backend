package mapper;

import model.entity.Transaction;
import model.home.*;

import org.mapstruct.*;

import java.time.*;

@Mapper(componentModel = "cdi",
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TransactionMapper {

    @Mapping(target = "txTime",
            source  = "txTime",
            qualifiedByName = "offsetToLocal")
    TxResponseDTO entityToResponse(Transaction t);

    @Mapping(target = "id",            ignore = true)
    @Mapping(target = "userSub",       ignore = true)
    @Mapping(target = "note",          ignore = true)
    @Mapping(target = "locationName",  ignore = true)
    @Mapping(target = "latitude",      ignore = true)
    @Mapping(target = "longitude",     ignore = true)
    Transaction quickRequestToEntity(QuickTxRequestDTO dto);

    @Mapping(target = "id",            ignore = true)
    @Mapping(target = "userSub",       ignore = true)
    @Mapping(target = "locationName",  ignore = true)
    @Mapping(target = "txTime",
            source  = "txTime",
            qualifiedByName = "localToOffset")
    Transaction fullRequestToEntity(FullTxRequestDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",            ignore = true)
    @Mapping(target = "userSub",       ignore = true)
    @Mapping(target = "locationName",  ignore = true)
    @Mapping(target = "latitude",      ignore = true)
    @Mapping(target = "longitude",     ignore = true)
    @Mapping(target = "txTime",
            source  = "txTime",
            qualifiedByName = "localToOffset")
    void updateFromFullDto(FullTxRequestDTO dto,
                           @MappingTarget Transaction entity);

    @Named("offsetToLocal")
    static LocalDateTime offsetToLocal(OffsetDateTime odt) {
        return odt == null ? null : odt.toLocalDateTime();
    }

    @Named("localToOffset")
    static OffsetDateTime localToOffset(LocalDateTime ldt) {
        return ldt == null ? null : ldt.atOffset(ZoneOffset.UTC);
    }
}
