package resource;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import jakarta.ws.rs.core.Response;
import model.notification.DeregisterDTO;
import model.notification.RegisterDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import service.NotificationService;
import utils.NoDbProfileWithPermitOidc;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

@QuarkusTest
@TestHTTPEndpoint(NotificationResource.class)
@TestProfile(NoDbProfileWithPermitOidc.class)
class NotificationResourceTest {

    @InjectMock
    NotificationService notifications;

    @BeforeAll
    static void setup() {
        RestAssured.basePath = "/api/notifications";
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void register_ReturnsNoContent_WhenValidSubscriptionId() {
        doNothing().when(notifications).registerSubscriptionForUser(anyString(), anyString());

        given()
                .contentType("application/json")
                .body(new RegisterDTO("sub-1", null))
                .when()
                .post("/register")
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void register_ReturnsBadRequest_WhenMissingSubscriptionId() {
        given()
                .contentType("application/json")
                .body(new RegisterDTO("", null))
                .when()
                .post("/register")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void deregister_ReturnsNoContent_WhenValidUser() {
        doNothing().when(notifications).deregisterForUser(anyString());

        given()
                .contentType("application/json")
                .body(new DeregisterDTO(null))
                .when()
                .post("/deregister")
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test
    void deregister_ReturnsUnauthorized_WhenUserNotAuthenticated() {
        given()
                .contentType("application/json")
                .body(new DeregisterDTO("sub-1"))
                .when()
                .post("/deregister")
                .then()
                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());
    }
}
