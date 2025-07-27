package service;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import repository.UserPushTokenRepository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Logger;

@ApplicationScoped
public class NotificationService {

    @Inject
    UserPushTokenRepository tokenRepo;

    @ConfigProperty(name = "onesignal.api.key")
    String apiKey;

    @ConfigProperty(name = "onesignal.app.id")
    String appId;

    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Logger LOGGER = Logger.getLogger(NotificationService.class.getName());

    public void sendToUser(String userSub, String title, String message) {
        tokenRepo.findPlayerId(userSub).ifPresent(playerId -> {
            JsonObject body = new JsonObject()
                    .put("app_id", appId)
                    .put("include_player_ids", new JsonArray().add(playerId))
                    .put("headings", new JsonObject().put("en", title))
                    .put("contents", new JsonObject().put("en", message));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://onesignal.com/api/v1/notifications"))
                    .header("Authorization", "Basic " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.encode()))
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 400) {
                    LOGGER.warning("Failed to send notification: " + response.body());
                    return;
                }

                JsonObject json = new JsonObject(response.body());

                if (json.containsKey("errors") && json.containsKey("invalid_player_ids")) {
                    JsonArray invalidIds = json.getJsonArray("invalid_player_ids");
                    for (int i = 0; i < invalidIds.size(); i++) {
                        String invalid = invalidIds.getString(i);
                        LOGGER.info("Removing invalid OneSignal player ID: " + invalid);
                        tokenRepo.deleteByPlayerId(invalid);
                    }
                }

            } catch (Exception e) {
                LOGGER.warning("Error sending push notification: " + e.getMessage());
            }
        });
    }
}
