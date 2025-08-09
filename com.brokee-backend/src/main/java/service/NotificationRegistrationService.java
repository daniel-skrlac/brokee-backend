package service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import repository.UserPushTokenRepository;

@ApplicationScoped
public class NotificationRegistrationService {

    @Inject
    UserPushTokenRepository repo;

    @Transactional
    public void registerPlayerId(String userSub, String playerId) {
        repo.upsert(userSub, playerId);
    }

    @Transactional
    public void deregisterPlayerId(String userSub) {
        repo.deleteByUserSub(userSub);
    }
}
