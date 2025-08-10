package resource;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import jakarta.ws.rs.core.Response;
import model.response.ServiceResponseDTO;
import model.transaction.PlannedTxRequestDTO;
import model.transaction.PlannedTxResponseDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import service.PlannedTxService;
import utils.NoDbProfileWithPermitOidc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestHTTPEndpoint(PlannedTxResource.class)
@TestProfile(NoDbProfileWithPermitOidc.class)
class PlannedTxResourceTest {

    @InjectMock
    PlannedTxService service;

    @BeforeAll
    static void setup() {
        RestAssured.basePath = "/api/planned-transactions";
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void listNonPaged_ReturnsOk() {
        var dto = new PlannedTxResponseDTO();
        dto.id = 1L;
        dto.type = "E";
        dto.categoryId = 5L;
        dto.title = "Rent";
        dto.amount = BigDecimal.valueOf(500);
        dto.dueDate = LocalDate.now();
        dto.autoBook = false;

        var resp = new ServiceResponseDTO<List<PlannedTxResponseDTO>>();
        resp.setSuccess(true);
        resp.setData(List.of(dto));
        resp.setStatusCode(200);

        when(service.list(anyString(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(resp);

        given()
                .queryParam("title", "Rent")
                .queryParam("type", "E")
                .when().get()
                .then().statusCode(200)
                .body("success", equalTo(true))
                .body("data[0].title", equalTo("Rent"));
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void list_ReturnsBadRequest_OnInvalidDate() {
        given()
                .queryParam("dueFrom", "not-a-date")
                .when().get()
                .then().statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body("success", equalTo(false));
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void getOne_ReturnsOk() {
        var dto = new PlannedTxResponseDTO();
        dto.id = 3L;
        dto.type = "E";
        dto.categoryId = 7L;
        dto.title = "Internet";
        dto.amount = BigDecimal.valueOf(50);
        dto.dueDate = LocalDate.now();
        dto.autoBook = false;

        var resp = new ServiceResponseDTO<PlannedTxResponseDTO>();
        resp.setSuccess(true);
        resp.setData(dto);
        resp.setStatusCode(200);

        when(service.getById(anyString(), eq(3L))).thenReturn(resp);

        given()
                .when().get("/3")
                .then().statusCode(200)
                .body("data.title", equalTo("Internet"));
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void create_ReturnsCreated() {
        var req = new PlannedTxRequestDTO();
        req.type = "E";
        req.categoryId = 8L;
        req.title = "Gym";
        req.amount = BigDecimal.valueOf(40);
        req.dueDate = LocalDate.now().plusDays(1);

        var dto = new PlannedTxResponseDTO();
        dto.id = 10L;
        dto.type = "E";
        dto.categoryId = 8L;
        dto.title = "Gym";
        dto.amount = BigDecimal.valueOf(40);
        dto.dueDate = req.dueDate;
        dto.autoBook = false;

        var resp = new ServiceResponseDTO<PlannedTxResponseDTO>();
        resp.setSuccess(true);
        resp.setData(dto);
        resp.setStatusCode(201);

        when(service.create(anyString(), any())).thenReturn(resp);

        given()
                .contentType("application/json")
                .body(req)
                .when().post()
                .then().statusCode(201)
                .body("data.title", equalTo("Gym"));
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void update_ReturnsOk() {
        var req = new PlannedTxRequestDTO();
        req.type = "E";
        req.categoryId = 8L;
        req.title = "Gym+";
        req.amount = BigDecimal.valueOf(45);
        req.dueDate = LocalDate.now().plusDays(2);

        var dto = new PlannedTxResponseDTO();
        dto.id = 10L;
        dto.type = "E";
        dto.categoryId = 8L;
        dto.title = "Gym+";
        dto.amount = BigDecimal.valueOf(45);
        dto.dueDate = req.dueDate;
        dto.autoBook = false;

        var resp = new ServiceResponseDTO<PlannedTxResponseDTO>();
        resp.setSuccess(true);
        resp.setData(dto);
        resp.setStatusCode(200);

        when(service.update(anyString(), eq(10L), any())).thenReturn(resp);

        given()
                .contentType("application/json")
                .body(req)
                .when().put("/10")
                .then().statusCode(200)
                .body("data.amount", equalTo(45));
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void delete_ReturnsOk() {
        var resp = new ServiceResponseDTO<Boolean>();
        resp.setSuccess(true);
        resp.setData(true);
        resp.setStatusCode(200);

        when(service.delete(anyString(), eq(10L))).thenReturn(resp);

        given()
                .when().delete("/10")
                .then().statusCode(200)
                .body("data", equalTo(true));
    }
}
