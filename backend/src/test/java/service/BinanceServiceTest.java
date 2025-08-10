package service;

import client.BinanceClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.InjectSpy;
import mapper.BinanceTokenMapper;
import model.entity.BinanceToken;
import model.external.BinanceTokenDTO;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import repository.BinanceTokenRepository;
import utils.NoDbProfile;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
@TestProfile(NoDbProfile.class)
class BinanceServiceTest {

    @InjectSpy
    BinanceService svc;

    @InjectMock @RestClient BinanceClient client;
    @InjectMock BinanceTokenRepository tokenRepo;
    @InjectMock BinanceTokenMapper mapper;

    @Test
    void saveCredentials_NoExistingToken_CreatesNewToken() {
        when(tokenRepo.findByUserSub("u1")).thenReturn(Optional.empty());
        var token = new BinanceToken(); token.setId(1L);
        when(tokenRepo.createForUser("u1", "api", "sec")).thenReturn(token);

        var dto = new BinanceTokenDTO(1L, "api", "sec", OffsetDateTime.now(), OffsetDateTime.now());
        when(mapper.entityToDto(token)).thenReturn(dto);

        var res = svc.saveCredentials("u1", "api", "sec");

        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getData()).isEqualTo(dto);
        verify(tokenRepo).createForUser("u1", "api", "sec");
    }

    @Test
    void saveCredentials_ExistingToken_UpdatesKeys() {
        var existing = new BinanceToken(); existing.setId(9L);
        when(tokenRepo.findByUserSub("u1")).thenReturn(Optional.of(existing));
        var updated = new BinanceToken(); updated.setId(9L);

        when(tokenRepo.updateKeys(9L, "api", "sec")).thenReturn(updated);
        var dto = new BinanceTokenDTO(9L, "api", "sec", OffsetDateTime.now(), OffsetDateTime.now());
        when(mapper.entityToDto(updated)).thenReturn(dto);

        var res = svc.saveCredentials("u1", "api", "sec");

        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getData()).isEqualTo(dto);
        verify(tokenRepo).updateKeys(9L, "api", "sec");
    }

    @Test
    void getCredentials_TokenExists_ReturnsDto() {
        var token = new BinanceToken(); token.setId(3L);
        var dto = new BinanceTokenDTO(3L, "a", "s", OffsetDateTime.now(), OffsetDateTime.now());

        when(tokenRepo.findByUserSub("u")).thenReturn(Optional.of(token));
        when(mapper.entityToDto(token)).thenReturn(dto);

        var res = svc.getCredentials("u");

        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getData().getId()).isEqualTo(3L);
    }

    @Test
    void getCredentials_TokenMissing_ReturnsFailure() {
        when(tokenRepo.findByUserSub("u")).thenReturn(Optional.empty());

        var res = svc.getCredentials("u");

        assertThat(res.isSuccess()).isFalse();
    }

    @Test
    void deleteCredentials_TokenNotDeleted_ReturnsFailure() {
        when(tokenRepo.deleteByUserSub("u")).thenReturn(false);

        var res = svc.deleteCredentials("u");

        assertThat(res.isSuccess()).isFalse();
    }

    @Test
    void getPortfolio_NoCredentials_ReturnsFailure() {
        when(tokenRepo.findByUserSub("u")).thenReturn(Optional.empty());

        var res = svc.getPortfolio("u", "EUR");

        assertThat(res.isSuccess()).isFalse();
    }

    @Test
    void getPortfolio_MarketDataFetchFails_ReturnsFailure() {
        var token = new BinanceToken(); token.setApiKey("k"); token.setSecretKey("s");
        when(tokenRepo.findByUserSub("u")).thenReturn(Optional.of(token));
        when(client.get24hrTicker()).thenThrow(new RuntimeException("boom"));

        var res = svc.getPortfolio("u", "EUR");

        assertThat(res.isSuccess()).isFalse();
    }

    @Test
    void getPortfolio_AccountFetchFails_ReturnsFailure() {
        var token = new BinanceToken(); token.setApiKey("k"); token.setSecretKey("s");
        when(tokenRepo.findByUserSub("u")).thenReturn(Optional.of(token));
        when(client.get24hrTicker()).thenReturn(java.util.List.of());
        when(client.getAccount(anyString(), anyInt(), anyLong(), anyString()))
                .thenThrow(new RuntimeException("nope"));

        var res = svc.getPortfolio("u", "EUR");

        assertThat(res.isSuccess()).isFalse();
    }
}
