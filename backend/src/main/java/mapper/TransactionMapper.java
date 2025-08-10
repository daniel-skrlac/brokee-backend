package mapper;

import model.entity.Transaction;
import model.home.*;

import org.mapstruct.*;

import java.time.*;

/** Maps Transaction ⇄ DTOs */
@Mapper(componentModel = "cdi",
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TransactionMapper {

    /* ════════════════════════════════════════════════
       ENTITY ➜ DTO    (OffsetDateTime → LocalDateTime)
       ════════════════════════════════════════════════ */
    @Mapping(target = "txTime",
            source  = "txTime",
            qualifiedByName = "offsetToLocal")
    TxResponseDTO entityToResponse(Transaction t);

    /* ════════════════════════════════════════════════
       QUICK-ADD DTO ➜ ENTITY
       (types already match → no qualifier)
       ════════════════════════════════════════════════ */
    @Mapping(target = "id",            ignore = true)
    @Mapping(target = "userSub",       ignore = true)
    @Mapping(target = "note",          ignore = true)
    @Mapping(target = "locationName",  ignore = true)
    @Mapping(target = "latitude",      ignore = true)
    @Mapping(target = "longitude",     ignore = true)
    // txTime maps 1-to-1 (OffsetDateTime → OffsetDateTime)
    Transaction quickRequestToEntity(QuickTxRequestDTO dto);

    /* ════════════════════════════════════════════════
       FULL DTO ➜ ENTITY    (LocalDateTime → OffsetDateTime)
       ════════════════════════════════════════════════ */
    @Mapping(target = "id",            ignore = true)
    @Mapping(target = "userSub",       ignore = true)
    @Mapping(target = "locationName",  ignore = true)
    @Mapping(target = "txTime",
            source  = "txTime",
            qualifiedByName = "localToOffset")
    Transaction fullRequestToEntity(FullTxRequestDTO dto);

    /* ════════════════════════════════════════════════
       PATCH UPDATE    (LocalDateTime → OffsetDateTime)
       ════════════════════════════════════════════════ */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",            ignore = true)
    @Mapping(target = "userSub",       ignore = true)
    @Mapping(target = "note",          ignore = true)
    @Mapping(target = "locationName",  ignore = true)
    @Mapping(target = "latitude",      ignore = true)
    @Mapping(target = "longitude",     ignore = true)
    @Mapping(target = "txTime",
            source  = "txTime",
            qualifiedByName = "localToOffset")
    void updateFromFullDto(FullTxRequestDTO dto,
                           @MappingTarget Transaction entity);

    /* ───── helpers ───── */

    /** OffsetDateTime → LocalDateTime (entity → DTO) */
    @Named("offsetToLocal")
    static LocalDateTime offsetToLocal(OffsetDateTime odt) {
        return odt == null ? null : odt.toLocalDateTime();
    }

    /** LocalDateTime → OffsetDateTime (DTO → entity) */
    @Named("localToOffset")
    static OffsetDateTime localToOffset(LocalDateTime ldt) {
        return ldt == null ? null : ldt.atOffset(ZoneOffset.UTC);
    }
}
