package resource;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import model.helper.PagedResponseDTO;
import model.home.FullTxRequestDTO;
import model.home.QuickTxRequestDTO;
import model.home.TxResponseDTO;
import model.response.ServiceResponseDTO;
import model.tracking.CategoryBreakdownDTO;
import model.tracking.LocationDTO;
import model.tracking.SpendingVsIncomeDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import service.TransactionService;
import utils.NoDbProfileWithPermitOidc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestHTTPEndpoint(TransactionResource.class)
@TestProfile(NoDbProfileWithPermitOidc.class)
class TransactionResourceTest {

    @InjectMock
    TransactionService txService;

    @BeforeAll
    static void setup() {
        RestAssured.basePath = "/api/transactions";
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void recent_ReturnsOk() {
        var dto = new TxResponseDTO(1L, "E", BigDecimal.TEN, 1L, LocalDateTime.now(), null, null);
        var resp = new ServiceResponseDTO<List<TxResponseDTO>>();
        resp.setSuccess(true);
        resp.setData(List.of(dto));
        resp.setStatusCode(200);

        when(txService.recent(anyString(), anyInt())).thenReturn(resp);

        given().queryParam("limit", 3)
                .when().get("/recent")
                .then().statusCode(200)
                .body("data.size()", equalTo(1));
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void page_ReturnsOk() {
        var page = new PagedResponseDTO<TxResponseDTO>(List.of(), 0, 10, 0L);
        var resp = new ServiceResponseDTO<PagedResponseDTO<TxResponseDTO>>();
        resp.setSuccess(true);
        resp.setData(page);
        resp.setStatusCode(200);

        when(txService.page(anyString(), anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(resp);

        given().queryParam("page", 0).queryParam("size", 10)
                .when().get()
                .then().statusCode(200)
                .body("success", equalTo(true));
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void balance_ReturnsOk() {
        var resp = new ServiceResponseDTO<BigDecimal>();
        resp.setSuccess(true);
        resp.setData(new BigDecimal("123.45"));
        resp.setStatusCode(200);

        when(txService.getBalance(anyString())).thenReturn(resp);

        given().when().get("/balance")
                .then().statusCode(200)
                .body("data", equalTo(123.45f));
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void range_ReturnsOk() {
        var dto = new TxResponseDTO(2L, "I", new BigDecimal("50.00"), 1L, LocalDateTime.now(), null, null);
        var resp = new ServiceResponseDTO<List<TxResponseDTO>>();
        resp.setSuccess(true);
        resp.setData(List.of(dto));
        resp.setStatusCode(200);

        when(txService.findByDateRange(
                anyString(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
        )).thenReturn(resp);

        given().queryParam("from", OffsetDateTime.now().minusDays(2).toString())
                .queryParam("to", OffsetDateTime.now().toString())
                .when().get("/range")
                .then().statusCode(200)
                .body("data.size()", equalTo(1));
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void dailyGraph_ReturnsOk() {
        var resp = new ServiceResponseDTO<Map<String, BigDecimal>>();
        resp.setSuccess(true);
        resp.setData(Map.of("2025-01-01", BigDecimal.TEN));
        resp.setStatusCode(200);

        when(txService.findDailyExpenses(anyString(), anyInt())).thenReturn(resp);

        given().queryParam("days", 7)
                .when().get("/graph/daily")
                .then().statusCode(200)
                .body("data.'2025-01-01'", equalTo(10));
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void monthlyGraph_ReturnsOk() {
        var resp = new ServiceResponseDTO<Map<String, BigDecimal>>();
        resp.setSuccess(true);
        resp.setData(Map.of("2025-01", BigDecimal.ONE));
        resp.setStatusCode(200);

        when(txService.findMonthlyExpenses(anyString(), anyInt())).thenReturn(resp);

        given().queryParam("year", 2025)
                .when().get("/graph/monthly")
                .then().statusCode(200)
                .body("data.'2025-01'", equalTo(1));
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void spendingVsIncome_ReturnsOk() {
        var resp = new ServiceResponseDTO<List<SpendingVsIncomeDTO>>();
        resp.setSuccess(true);
        resp.setData(List.of(new SpendingVsIncomeDTO("JAN", BigDecimal.ONE, BigDecimal.TEN)));
        resp.setStatusCode(200);

        when(txService.spendingVsIncome(anyString(), anyInt())).thenReturn(resp);

        given().queryParam("year", 2025)
                .when().get("/spending-vs-income")
                .then().statusCode(200)
                .body("data[0].month", equalTo("JAN"));
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void categoryBreakdown_ReturnsOk() {
        var resp = new ServiceResponseDTO<List<CategoryBreakdownDTO>>();
        resp.setSuccess(true);
        resp.setData(List.of(new CategoryBreakdownDTO("Food", BigDecimal.TEN)));
        resp.setStatusCode(200);

        when(txService.categoryBreakdown(anyString(), anyString())).thenReturn(resp);

        given().queryParam("month", "2025-01")
                .when().get("/category-breakdown")
                .then().statusCode(200)
                .body("data[0].category", equalTo("Food"));
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void topLocations_ReturnsOk() {
        var resp = new ServiceResponseDTO<List<LocationDTO>>();
        resp.setSuccess(true);
        resp.setData(List.of(new LocationDTO(
                new BigDecimal("44.8"),
                new BigDecimal("20.5"),
                "Cafe",
                new BigDecimal("30.00")
        )));
        resp.setStatusCode(200);

        when(txService.topLocations(anyString(), anyInt())).thenReturn(resp);

        given().queryParam("limit", 3)
                .when().get("/top-locations")
                .then().statusCode(200)
                .body("data[0].label", equalTo("Cafe"));
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void getById_ReturnsOk() {
        var dto = new TxResponseDTO(5L, "E", BigDecimal.ONE, 1L, LocalDateTime.now(), null, null);
        var resp = new ServiceResponseDTO<TxResponseDTO>();
        resp.setSuccess(true);
        resp.setData(dto);
        resp.setStatusCode(200);

        when(txService.findById(anyString(), eq(5L))).thenReturn(resp);

        given().when().get("/5")
                .then().statusCode(200)
                .body("data.id", equalTo(5));
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void quickAdd_ReturnsCreated() {
        var req = new QuickTxRequestDTO(
                "E",
                new BigDecimal("3.50"),
                1L,
                OffsetDateTime.now()
        );
        var dto = new TxResponseDTO(11L, "E", new BigDecimal("3.50"), 1L, LocalDateTime.now(), null, null);
        var resp = new ServiceResponseDTO<TxResponseDTO>();
        resp.setSuccess(true);
        resp.setData(dto);
        resp.setStatusCode(201);

        when(txService.quickAdd(anyString(), any())).thenReturn(resp);

        given().contentType("application/json").body(req)
                .when().post("/quick")
                .then().statusCode(201)
                .body("data.id", equalTo(11));
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void create_ReturnsCreated() {
        var req = new FullTxRequestDTO(
                "E",
                BigDecimal.TEN,
                1L,
                LocalDateTime.now(),
                "note",
                null,
                null
        );
        var dto = new TxResponseDTO(12L, "E", BigDecimal.TEN, 1L, LocalDateTime.now(), null, "note");
        var resp = new ServiceResponseDTO<TxResponseDTO>();
        resp.setSuccess(true);
        resp.setData(dto);
        resp.setStatusCode(201);

        when(txService.create(anyString(), any())).thenReturn(resp);

        given().contentType("application/json").body(req)
                .when().post()
                .then().statusCode(201)
                .body("data.id", equalTo(12));
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void patch_ReturnsOk() {
        var req = new FullTxRequestDTO(
                "E",
                new BigDecimal("12.00"),
                1L,
                LocalDateTime.now(),
                "note+",
                null,
                null
        );
        var dto = new TxResponseDTO(12L, "E", new BigDecimal("12.00"), 1L, LocalDateTime.now(), null, "note+");
        var resp = new ServiceResponseDTO<TxResponseDTO>();
        resp.setSuccess(true);
        resp.setData(dto);
        resp.setStatusCode(200);

        when(txService.update(anyString(), eq(12L), any())).thenReturn(resp);

        given().contentType("application/json").body(req)
                .when().patch("/12")
                .then().statusCode(200)
                .body("data.note", equalTo("note+"));
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void delete_ReturnsOk() {
        var resp = new ServiceResponseDTO<Boolean>();
        resp.setSuccess(true);
        resp.setData(true);
        resp.setStatusCode(200);

        when(txService.delete(anyString(), eq(12L))).thenReturn(resp);

        given().when().delete("/12")
                .then().statusCode(200)
                .body("data", equalTo(true));
    }
}
