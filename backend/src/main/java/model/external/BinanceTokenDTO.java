package model.external;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor
public class BinanceTokenDTO {
    private Long id;
    private String apiKey;
    private String secretKey;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
