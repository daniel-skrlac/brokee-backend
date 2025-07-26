package mapper;

import model.entity.Transaction;
import model.home.FullTxRequestDTO;
import model.home.QuickTxRequestDTO;
import model.home.TxResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "cdi")
public interface TransactionMapper {

    TxResponseDTO entityToResponse(Transaction t);

    @Mapping(target = "id", ignore = true)
    Transaction quickRequestToEntity(QuickTxRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    Transaction fullRequestToEntity(FullTxRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    void updateFromFullDto(FullTxRequestDTO dto, @MappingTarget Transaction entity);
}
