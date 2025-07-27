package repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import model.entity.BinanceToken;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class BinanceTokenRepository implements PanacheRepository<BinanceToken> {

    @Inject
    EntityManager em;

    public BinanceToken createForUser(String userSub, String apiKey, String secretKey) {
        BinanceToken token = new BinanceToken();
        token.setUserSub(userSub);
        token.setApiKey(apiKey);
        token.setSecretKey(secretKey);
        persist(token);
        return token;
    }

    public Optional<BinanceToken> findByUserSub(String userSub) {
        return find("userSub", userSub).firstResultOptional();
    }

    public BinanceToken updateKeys(Long id, String newApiKey, String newSecretKey) {
        BinanceToken token = findById(id);
        if (token == null) {
            throw new IllegalArgumentException("No BinanceToken with id " + id);
        }
        token.setApiKey(newApiKey);
        token.setSecretKey(newSecretKey);
        return getEntityManager().merge(token);
    }

    public boolean deleteByUserSub(String userSub) {
        return delete("userSub", userSub) > 0;
    }

    public List<String> findAllUsersWithBinanceTokens() {
        return em.createQuery("SELECT DISTINCT t.userSub FROM BinanceToken t", String.class)
                .getResultList();
    }
}
