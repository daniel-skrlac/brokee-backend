package service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.InjectSpy;
import org.junit.jupiter.api.Test;
import repository.UserPushTokenRepository;
import utils.NoDbProfile;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(NoDbProfile.class)
class NotificationServiceTest {

    @InjectSpy
    NotificationService svc;

    @InjectMock
    UserPushTokenRepository repo;

    @Test
    void registerSubscriptionForUser_ValidInput_DelegatesToRepository() {
        svc.registerSubscriptionForUser("u", "s1");
        verify(repo).upsert("u", "s1");
    }

    @Test
    void deregisterForUser_ValidUser_DelegatesToRepository() {
        svc.deregisterForUser("u");
        verify(repo).deleteByUserSub("u");
    }

    @Test
    void deregisterBySubscriptionId_ValidId_DelegatesToRepository() {
        svc.deregisterBySubscriptionId("s1");
        verify(repo).deleteByPlayerId("s1");
    }

    @Test
    void sendToUser_NoSubscription_DoesNotCallSendToSubscription() {
        when(repo.findByUserSub("u")).thenReturn(Optional.empty());

        svc.sendToUser("u", "t", "m");

        verify(svc, never()).sendToSubscription(anyString(), anyString(), anyString());
    }

    @Test
    void sendToUser_SubscriptionExists_DelegatesToSendToSubscription() {
        when(repo.findByUserSub("u")).thenReturn(Optional.of("sub-123"));
        doNothing().when(svc).sendToSubscription(anyString(), anyString(), anyString());

        svc.sendToUser("u", "Hello", "World");

        verify(svc).sendToSubscription(eq("sub-123"), eq("Hello"), eq("World"));
    }
}
