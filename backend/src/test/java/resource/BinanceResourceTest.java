package resource;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import jakarta.ws.rs.core.Response;
import model.external.BinanceTokenDTO;
import model.response.ServiceResponseDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import service.BinanceService;
import utils.NoDbProfileWithPermitOidc;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestHTTPEndpoint(BinanceResource.class)
@TestProfile(NoDbProfileWithPermitOidc.class)
class BinanceResourceTest {

    @InjectMock
    BinanceService binanceService;

    @BeforeAll
    static void setupRestAssured() {
        RestAssured.basePath = "/api/binance";
    }

    @TestSecurity(user = "dskrlac", roles = "user")
    @Test
    void testGetCredentials_ReturnsOk_WhenCredentialsExist() {
        ServiceResponseDTO<BinanceTokenDTO> responseDTO = new ServiceResponseDTO<>();
        responseDTO.setSuccess(true);
        responseDTO.setData(new BinanceTokenDTO(1L, "api-key", "secret-key", null, null));
        responseDTO.setStatusCode(200);

        when(binanceService.getCredentials(anyString())).thenReturn(responseDTO);

        given()
                .when()
                .get()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("success", equalTo(true))
                .body("data.apiKey", equalTo("api-key"));
    }

    @TestSecurity(user = "user2", roles = "user")
    @Test
    void testGetNCredentials_ReturnsOk_WhenCredentialsExist() {
        BinanceTokenDTO tokenDTO = new BinanceTokenDTO(1L, "api-key", "secret-key", null, null);

        ServiceResponseDTO<BinanceTokenDTO> responseDTO = new ServiceResponseDTO<>();
        responseDTO.setSuccess(true);
        responseDTO.setData(tokenDTO);
        responseDTO.setStatusCode(200);

        when(binanceService.getCredentials(anyString())).thenReturn(responseDTO);

        given()
                .when()
                .get()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("success", equalTo(true))
                .body("data.apiKey", equalTo("api-key"));
    }

    @TestSecurity(user = "dskrlac", roles = "user")
    @Test
    void testDeleteCredentials_ReturnsOk_WhenCredentialsExist() {
        ServiceResponseDTO<BinanceTokenDTO> responseDTO = new ServiceResponseDTO<>();
        responseDTO.setSuccess(true);
        responseDTO.setData(null);
        responseDTO.setStatusCode(200);

        when(binanceService.deleteCredentials(anyString())).thenReturn(responseDTO);

        given()
                .when()
                .delete()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("success", equalTo(true));
    }

    @TestSecurity(user = "dskrlac", roles = "user")
    @Test
    void testDeleteCredentials_ReturnsNotFound_WhenCredentialsDoNotExist() {
        ServiceResponseDTO<BinanceTokenDTO> responseDTO = new ServiceResponseDTO<>();
        responseDTO.setSuccess(false);
        responseDTO.setStatusCode(404);

        when(binanceService.deleteCredentials(anyString())).thenReturn(responseDTO);

        given()
                .when()
                .delete()
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode())
                .body("success", equalTo(false))
                .body("data", equalTo(null));
    }
}
