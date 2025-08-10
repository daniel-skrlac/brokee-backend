package resource;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import jakarta.ws.rs.core.Response;
import model.response.ServiceResponseDTO;
import model.settings.SavingsGoalRequestDTO;
import model.settings.SavingsGoalResponseDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import service.SavingsGoalService;
import utils.NoDbProfileWithPermitOidc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestHTTPEndpoint(SavingsGoalResource.class)
@TestProfile(NoDbProfileWithPermitOidc.class)
class SavingsGoalResourceTest {

    @InjectMock
    SavingsGoalService svc;

    @BeforeAll
    static void setup() {
        RestAssured.basePath = "/api/savings";
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void get_ReturnsOk() {
        var dto = new SavingsGoalResponseDTO();
        dto.userSub = "user1";
        dto.targetAmt = BigDecimal.valueOf(1000.00);
        dto.targetDate = LocalDate.now().plusMonths(6);

        var resp = new ServiceResponseDTO<SavingsGoalResponseDTO>();
        resp.setSuccess(true);
        resp.setData(dto);
        resp.setStatusCode(200);

        when(svc.get(anyString())).thenReturn(resp);

        given()
                .when().get()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("success", equalTo(true))
                .body("data.targetAmt", equalTo(1000.0f));
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void upsert_ReturnsOk() {
        // Build request DTO via no-args + field sets OR send JSON
        var req = new SavingsGoalRequestDTO();
        req.targetAmt = BigDecimal.valueOf(1200.00);
        req.targetDate = LocalDate.now().plusMonths(9);

        var out = new SavingsGoalResponseDTO();
        out.userSub = "user1";
        out.targetAmt = req.targetAmt;
        out.targetDate = req.targetDate;

        var resp = new ServiceResponseDTO<SavingsGoalResponseDTO>();
        resp.setSuccess(true);
        resp.setData(out);
        resp.setStatusCode(200);

        when(svc.upsert(anyString(), any())).thenReturn(resp);

        given()
                .contentType("application/json")
                .body(req)
                .when().post()
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.targetAmt", equalTo(1200.0f));
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void patch_ReturnsOk() {
        var req = new SavingsGoalRequestDTO();
        req.targetAmt = BigDecimal.valueOf(1500.00);
        req.targetDate = LocalDate.now().plusMonths(12);

        var out = new SavingsGoalResponseDTO();
        out.userSub = "user1";
        out.targetAmt = req.targetAmt;
        out.targetDate = req.targetDate;

        var resp = new ServiceResponseDTO<SavingsGoalResponseDTO>();
        resp.setSuccess(true);
        resp.setData(out);
        resp.setStatusCode(200);

        when(svc.patch(anyString(), any())).thenReturn(resp);

        given()
                .contentType("application/json")
                .body(req)
                .when().patch()
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.targetAmt", equalTo(1500.0f));
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void delete_ReturnsOk() {
        var resp = new ServiceResponseDTO<Boolean>();
        resp.setSuccess(true);
        resp.setData(true);
        resp.setStatusCode(200);

        when(svc.delete(anyString())).thenReturn(resp);

        given()
                .when().delete()
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data", equalTo(true));
    }
}
