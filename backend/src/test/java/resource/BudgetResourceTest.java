package resource;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import jakarta.ws.rs.core.Response;
import model.helper.PagedResponseDTO;
import model.home.BudgetRequestDTO;
import model.home.BudgetResponseDTO;
import model.response.ServiceResponseDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import service.BudgetService;
import utils.NoDbProfileWithPermitOidc;

import java.math.BigDecimal;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestHTTPEndpoint(BudgetResource.class)
@TestProfile(NoDbProfileWithPermitOidc.class)
class BudgetResourceTest {

    @InjectMock
    BudgetService svc;

    @BeforeAll
    static void setupRestAssured() {
        RestAssured.basePath = "/api/budgets";
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void list_ReturnsOk_WhenValidRequest() {
        var page = new PagedResponseDTO<BudgetResponseDTO>(List.of(), 0, 10, 0L);
        var resp = new ServiceResponseDTO<PagedResponseDTO<BudgetResponseDTO>>();
        resp.setSuccess(true);
        resp.setData(page);
        resp.setStatusCode(200);

        when(svc.list(anyString(), anyInt(), anyInt())).thenReturn(resp);

        given()
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("success", equalTo(true));
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void createBulk_ReturnsOk_WhenValidPayload() {
        var dto = new BudgetRequestDTO(1L, BigDecimal.valueOf(100));
        var out = List.of(new BudgetResponseDTO(1L, BigDecimal.valueOf(100)));
        var resp = new ServiceResponseDTO<List<BudgetResponseDTO>>();
        resp.setSuccess(true);
        resp.setData(out);
        resp.setStatusCode(200);

        when(svc.bulkCreate(anyString(), any())).thenReturn(resp);

        given()
                .contentType("application/json")
                .body(List.of(dto))
                .when()
                .post("/bulk")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("success", equalTo(true))
                .body("data[0].categoryId", equalTo(1));
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void patchBulk_ReturnsOk_WhenValidPayload() {
        var dto = new BudgetRequestDTO(1L, BigDecimal.valueOf(120));
        var out = List.of(new BudgetResponseDTO(1L, BigDecimal.valueOf(120)));
        var resp = new ServiceResponseDTO<List<BudgetResponseDTO>>();
        resp.setSuccess(true);
        resp.setData(out);
        resp.setStatusCode(200);

        when(svc.bulkPatch(anyString(), any())).thenReturn(resp);

        given()
                .contentType("application/json")
                .body(List.of(dto))
                .when()
                .patch("/bulk")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("success", equalTo(true));
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void deleteBulk_ReturnsOk_WhenValidIdsProvided() {
        var resp = new ServiceResponseDTO<Long>();
        resp.setSuccess(true);
        resp.setData(2L);
        resp.setStatusCode(200);

        when(svc.deleteBulk(anyString(), any())).thenReturn(resp);

        given()
                .contentType("application/json")
                .body(List.of(1L, 2L))
                .when()
                .delete()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("success", equalTo(true))
                .body("data", equalTo(2));
    }
}
