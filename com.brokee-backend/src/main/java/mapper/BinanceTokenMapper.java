package mapper;

import model.entity.BinanceToken;
import model.external.BinanceTokenDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "cdi",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface BinanceTokenMapper {
    BinanceTokenDTO entityToDto(BinanceToken token);
}
