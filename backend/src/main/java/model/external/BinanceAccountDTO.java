package model.external;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BinanceAccountDTO {
    private List<BalanceDTO> balances;

    @Getter
    @Setter
    public static class BalanceDTO {
        private String asset;
        private String free;
        private String locked;
    }
}

