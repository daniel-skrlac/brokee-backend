package service;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import model.entity.BinanceToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repository.BinanceTokenRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@QuarkusTest
class BinanceServiceIT {

    @Inject
    BinanceService service;
    @Inject
    BinanceTokenRepository tokenRepo;

    @BeforeEach
    @Transactional
    void before() {
        tokenRepo.deleteAll();
    }

    @Test
    @TestTransaction
    void saveCredentials_creates_whenMissing() {
        var res = service.saveCredentials("u1", "apiKey", "secretKey");

        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getData()).isNotNull();
        assertThat(res.getData().getApiKey()).isEqualTo("apiKey");

        Optional<BinanceToken> saved = tokenRepo.findByUserSub("u1");
        assertThat(saved).isPresent();
        assertThat(saved.get().getSecretKey()).isEqualTo("secretKey");
    }

    @Test
    @TestTransaction
    void saveCredentials_updates_whenExists() {
        tokenRepo.createForUser("u1", "oldApi", "oldSec");

        var res = service.saveCredentials("u1", "newApi", "newSec");

        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getData().getApiKey()).isEqualTo("newApi");

        var inDb = tokenRepo.findByUserSub("u1");
        assertThat(inDb).isPresent();
        assertThat(inDb.get().getApiKey()).isEqualTo("newApi");
        assertThat(inDb.get().getSecretKey()).isEqualTo("newSec");
    }

    @Test
    @TestTransaction
    void getCredentials_returnsOk_whenExists() {
        tokenRepo.createForUser("u_get", "api", "sec");

        var res = service.getCredentials("u_get");

        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getData()).isNotNull();
        assertThat(res.getData().getApiKey()).isEqualTo("api");
    }

    @Test
    void getCredentials_returns404_whenMissing() {
        var res = service.getCredentials("nope");
        assertThat(res.isSuccess()).isFalse();
        assertThat(res.getStatusCode()).isEqualTo(404);
    }

    @Test
    @TestTransaction
    void deleteCredentials_returnsOk_andRemoves() {
        tokenRepo.createForUser("u_del", "api", "sec");

        var res = service.deleteCredentials("u_del");

        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getData()).isNotNull();
        assertThat(tokenRepo.findByUserSub("u_del")).isEmpty();
    }

    @Test
    void deleteCredentials_returns404_whenMissing() {
        var res = service.deleteCredentials("ghost");
        assertThat(res.isSuccess()).isFalse();
        assertThat(res.getStatusCode()).isEqualTo(404);
    }

    @Test
    void getPortfolio_returns404_whenNoCredentials() {
        var res = service.getPortfolio("missingUser", "EUR");
        assertThat(res.isSuccess()).isFalse();
        assertThat(res.getStatusCode()).isEqualTo(404);
    }

    @Test
    @TestTransaction
    void getPortfolio_marketDataFailure_returns500() {
        tokenRepo.createForUser("u_mkt_fail", "k", "s");

        var res = service.getPortfolio("u_mkt_fail", "EUR");

        assertThat(res.isSuccess()).isFalse();
        assertThat(res.getStatusCode()).isEqualTo(500);
    }

    @Test
    @TestTransaction
    void getPortfolio_accountFailure_returns500() {
        tokenRepo.createForUser("u_acc_fail", "k", "s");

        var res = service.getPortfolio("u_acc_fail", "EUR");

        assertThat(res.isSuccess()).isFalse();
        assertThat(res.getStatusCode()).isEqualTo(500);
    }
}
