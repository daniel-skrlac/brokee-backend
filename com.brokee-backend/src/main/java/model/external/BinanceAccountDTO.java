package model.external;

import lombok.Getter;

import java.util.List;

@Getter
public class BinanceAccountDTO {
    private List<BalanceDTO> balances;

    @Getter
    public static class BalanceDTO {
        private String asset;
        private String free;
        private String locked;
    }
}

