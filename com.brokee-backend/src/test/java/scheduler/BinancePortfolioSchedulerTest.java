package scheduler;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import model.external.FullPortfolioDTO;
import model.response.ServiceResponseDTO;
import org.junit.jupiter.api.Test;
import repository.BinanceTokenRepository;
import repository.CategoryRepository;
import repository.TransactionRepository;
import service.BinanceService;
import utils.NoDbProfile;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(NoDbProfile.class)
class BinancePortfolioSchedulerTest {

    @Inject
    BinancePortfolioScheduler scheduler;

    @InjectMock
    BinanceTokenRepository tokenRepo;
    @InjectMock
    BinanceService binanceService;
    @InjectMock
    CategoryRepository categoryRepo;
    @InjectMock
    TransactionRepository txRepo;

    @Test
    void snapshotBinancePortfolio_NoSnapshotExists_InsertsNewSnapshot() {
        when(tokenRepo.findAllUsersWithBinanceTokens()).thenReturn(List.of("u1"));
        when(categoryRepo.findIdByName("Investments")).thenReturn(77L);

        FullPortfolioDTO dto = mock(FullPortfolioDTO.class);
        when(dto.getTotalEurValue()).thenReturn(new BigDecimal("100"));
        @SuppressWarnings("unchecked")
        ServiceResponseDTO<FullPortfolioDTO> ok = mock(ServiceResponseDTO.class);
        when(ok.isSuccess()).thenReturn(true);
        when(ok.getData()).thenReturn(dto);
        when(binanceService.getPortfolio("u1", "EUR")).thenReturn(ok);

        when(txRepo.getLastSnapshotAmount("u1", 77L, "Binance Portfolio Snapshot")).thenReturn(null);
        when(txRepo.snapshotExists("u1", 77L, "Binance Portfolio Snapshot")).thenReturn(false);

        scheduler.snapshotBinancePortfolio();

        verify(txRepo).insertInvestmentSnapshot(eq("u1"), eq(new BigDecimal("100")), eq(77L), any(), anyString());
        verify(txRepo, never()).updateInvestmentSnapshot(any(), any(), any(), any(), any());
    }

    @Test
    void snapshotBinancePortfolio_SnapshotExistsAndValueChanged_UpdatesSnapshot() {
        when(tokenRepo.findAllUsersWithBinanceTokens()).thenReturn(List.of("u1"));
        when(categoryRepo.findIdByName("Investments")).thenReturn(77L);

        FullPortfolioDTO dto = mock(FullPortfolioDTO.class);
        when(dto.getTotalEurValue()).thenReturn(new BigDecimal("105.50"));
        @SuppressWarnings("unchecked")
        ServiceResponseDTO<FullPortfolioDTO> ok = mock(ServiceResponseDTO.class);
        when(ok.isSuccess()).thenReturn(true);
        when(ok.getData()).thenReturn(dto);
        when(binanceService.getPortfolio("u1", "EUR")).thenReturn(ok);

        when(txRepo.getLastSnapshotAmount("u1", 77L, "Binance Portfolio Snapshot")).thenReturn(new BigDecimal("100"));
        when(txRepo.snapshotExists("u1", 77L, "Binance Portfolio Snapshot")).thenReturn(true);

        scheduler.snapshotBinancePortfolio();

        verify(txRepo).updateInvestmentSnapshot(eq("u1"), eq(77L), anyString(), eq(new BigDecimal("105.50")), any());
        verify(txRepo, never()).insertInvestmentSnapshot(any(), any(), any(), any(), anyString());
    }

    @Test
    void snapshotBinancePortfolio_ResponseNotSuccessOrDataNull_DoesNothing() {
        when(tokenRepo.findAllUsersWithBinanceTokens()).thenReturn(List.of("u1"));
        when(categoryRepo.findIdByName("Investments")).thenReturn(77L);

        @SuppressWarnings("unchecked")
        ServiceResponseDTO<FullPortfolioDTO> bad = mock(ServiceResponseDTO.class);
        when(bad.isSuccess()).thenReturn(false);
        when(binanceService.getPortfolio("u1", "EUR")).thenReturn(bad);

        scheduler.snapshotBinancePortfolio();

        verify(txRepo, never()).insertInvestmentSnapshot(any(), any(), any(), any(), anyString());
        verify(txRepo, never()).updateInvestmentSnapshot(any(), any(), any(), any(), any());
    }
}

