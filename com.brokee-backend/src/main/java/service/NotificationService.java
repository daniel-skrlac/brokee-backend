package service;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import repository.UserPushTokenRepository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;

@ApplicationScoped
public class NotificationService {

    @Inject
    UserPushTokenRepository tokens;

    @ConfigProperty(name = "onesignal.api.key")
    String apiKey;

    @ConfigProperty(name = "onesignal.app.id")
    String appId;

    private static final Logger LOGGER = Logger.getLogger(NotificationService.class.getName());
    private static final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private static final URI NOTIFY_URI = URI.create("https://api.onesignal.com/notifications");

    @Transactional
    public void registerSubscriptionForUser(String userSub, String subscriptionId) {
        tokens.upsert(userSub, subscriptionId);
    }

    @Transactional
    public void deregisterForUser(String userSub) {
        tokens.deleteByUserSub(userSub);
    }

    @Transactional
    public void deregisterBySubscriptionId(String subscriptionId) {
        tokens.deleteByPlayerId(subscriptionId);
    }

    public void sendToUser(String userSub, String title, String message) {
        tokens.findByUserSub(userSub).ifPresentOrElse(
                subId -> sendToSubscription(subId, title, message),
                () -> LOGGER.info("No subscription id on file for user " + userSub + "; skipping push")
        );
    }

    public void sendToSubscription(String subscriptionId, String title, String message) {
        JsonObject body = new JsonObject()
                .put("app_id", appId)
                .put("target_channel", "push")
                .put("include_subscription_ids", new JsonArray().add(subscriptionId))
                .put("headings", new JsonObject().put("en", title))
                .put("contents", new JsonObject().put("en", message));
        post(body, "sendToSubscription");
    }

    public void sendToExternalId(String externalId, String title, String message) {
        JsonObject body = new JsonObject()
                .put("app_id", appId)
                .put("target_channel", "push")
                .put("include_aliases", new JsonObject()
                        .put("external_id", new JsonArray().add(externalId)))
                .put("headings", new JsonObject().put("en", title))
                .put("contents", new JsonObject().put("en", message));
        post(body, "sendToExternalId");
    }

    private void post(JsonObject body, String tag) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(NOTIFY_URI)
                .header("Authorization", "Key " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.encode()))
                .build();

        try {
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 400) {
                LOGGER.warning("OneSignal " + tag + " error " + res.statusCode() + ": " + res.body());
            } else {
                LOGGER.info("OneSignal " + tag + " ok " + res.statusCode() + ": " + res.body());
            }
        } catch (Exception e) {
            LOGGER.warning("OneSignal " + tag + " request failed: " + e.getMessage());
        }
    }
}
