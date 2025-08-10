package resource;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import jakarta.ws.rs.core.Response;
import model.response.ServiceResponseDTO;
import model.tracking.DashboardSummaryDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import service.DashboardService;
import utils.NoDbProfileWithPermitOidc;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestHTTPEndpoint(DashboardResource.class)
@TestProfile(NoDbProfileWithPermitOidc.class)
class DashboardResourceTest {

    @InjectMock
    DashboardService svc;

    @BeforeAll
    static void setup() {
        RestAssured.basePath = "/api/dashboard";
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void summary_ReturnsOk_WhenCalled() {
        var dto = new DashboardSummaryDTO(
                new BigDecimal("500.00"),
                new BigDecimal("1200.00"),
                new BigDecimal("700.00")
        );
        var resp = new ServiceResponseDTO<DashboardSummaryDTO>();
        resp.setSuccess(true);
        resp.setData(dto);
        resp.setStatusCode(200);

        when(svc.summary(anyString())).thenReturn(resp);

        given()
                .when()
                .get("/summary")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("success", equalTo(true))
                .body("data.totalExpenses", equalTo(500.00F))
                .body("data.totalIncome", equalTo(1200.00F))
                .body("data.budgetRemaining", equalTo(700.00F));
    }
}
