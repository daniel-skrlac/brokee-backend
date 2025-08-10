package resource;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import jakarta.ws.rs.core.Response;
import model.home.CategoryResponseDTO;
import model.response.ServiceResponseDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import service.CategoryService;
import utils.NoDbProfileWithPermitOidc;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestHTTPEndpoint(CategoryResource.class)
@TestProfile(NoDbProfileWithPermitOidc.class)
class CategoryResourceTest {

    @InjectMock
    CategoryService svc;

    @BeforeAll
    static void setup() {
        RestAssured.basePath = "/api/categories";
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void getAll_ReturnsOk_WhenNoNameProvided() {
        var resp = new ServiceResponseDTO<List<CategoryResponseDTO>>();
        resp.setSuccess(true);
        resp.setData(List.of(new CategoryResponseDTO(1L, "Food")));
        resp.setStatusCode(200);

        when(svc.listAll()).thenReturn(resp);

        given()
                .when()
                .get()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("success", equalTo(true))
                .body("data[0].name", equalTo("Food"));
    }

    @TestSecurity(user = "user1", roles = "user")
    @Test
    void getAll_ReturnsOk_WhenNameProvided() {
        var resp = new ServiceResponseDTO<List<CategoryResponseDTO>>();
        resp.setSuccess(true);
        resp.setData(List.of(new CategoryResponseDTO(2L, "Fuel")));
        resp.setStatusCode(200);

        when(svc.search(anyString())).thenReturn(resp);

        given()
                .queryParam("name", "Fu")
                .when()
                .get()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("success", equalTo(true))
                .body("data[0].name", equalTo("Fuel"));
    }
}
