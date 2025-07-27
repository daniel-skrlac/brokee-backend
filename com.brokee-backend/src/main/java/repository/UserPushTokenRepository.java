package repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

import java.util.Optional;

@ApplicationScoped
public class UserPushTokenRepository {

    @Inject
    EntityManager em;

    public void saveOrUpdate(String userSub, String playerId) {
        em.createNativeQuery("""
                            MERGE INTO user_push_token AS target
                            USING (SELECT ? AS user_sub, ? AS player_id) AS source
                            ON target.user_sub = source.user_sub
                            WHEN MATCHED THEN UPDATE SET target.player_id = source.player_id
                            WHEN NOT MATCHED THEN INSERT (user_sub, player_id) VALUES (source.user_sub, source.player_id)
                        """)
                .setParameter(1, userSub)
                .setParameter(2, playerId)
                .executeUpdate();
    }

    public void deleteByUserSub(String userSub) {
        em.createNativeQuery("""
                    DELETE FROM user_push_token WHERE user_sub = ?
                """).setParameter(1, userSub).executeUpdate();
    }

    public void deleteByPlayerId(String playerId) {
        em.createNativeQuery("DELETE FROM user_push_token WHERE player_id = ?")
                .setParameter(1, playerId)
                .executeUpdate();
    }

    public Optional<String> findPlayerId(String userSub) {
        try {
            return Optional.ofNullable((String) em.createNativeQuery("""
                        SELECT player_id FROM user_push_token WHERE user_sub = ?
                    """).setParameter(1, userSub).getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
}
