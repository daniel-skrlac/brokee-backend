package resource;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import mapper.TransactionMapper;
import model.entity.Transaction;
import model.home.TxResponseDTO;
import model.response.ServiceResponseDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import service.RevolutImportService;
import service.TransactionService;
import utils.NoDbProfileWithPermitOidc;

import java.math.BigDecimal;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestHTTPEndpoint(RevolutResource.class)
@TestProfile(NoDbProfileWithPermitOidc.class)
class RevolutResourceTest {

    @InjectMock
    RevolutImportService importSvc;
    @InjectMock
    TransactionService txService;
    @InjectMock
    TransactionMapper transactionMapper;

    @BeforeAll
    static void setup() {
        RestAssured.basePath = "/api/revolut";
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void testImportPdf_ReturnsCreated_WhenValidFile() {
        var entityTx = new Transaction();
        entityTx.setId(1L);

        var resp = new ServiceResponseDTO<List<Transaction>>();
        resp.setSuccess(true);
        resp.setData(List.of(entityTx));
        resp.setStatusCode(201);

        when(importSvc.importMonthlyPdf(anyString(), any())).thenReturn(resp);
        when(transactionMapper.entityToResponse(any(Transaction.class))).thenReturn(
                new TxResponseDTO(1L, "EXPENSE", BigDecimal.valueOf(50), 2L, null, null, null)
        );

        given()
                .multiPart("file", "revolut.pdf", "dummy".getBytes())
                .when().post("/import")
                .then()
                .statusCode(201)
                .body("success", equalTo(true))
                .body("data.size()", equalTo(1));
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void testImportPdf_ReturnsBadRequest_WhenServiceThrowsException() throws Exception {
        when(importSvc.importMonthlyPdf(anyString(), any()))
                .thenThrow(new RuntimeException("boom"));

        given()
                .multiPart("file", "revolut.pdf", "dummy".getBytes())
                .when().post("/import")
                .then()
                .statusCode(400)
                .body("success", equalTo(false));
    }
}
