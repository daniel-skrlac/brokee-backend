package resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import model.helper.PagedResponseDTO;
import model.home.BudgetRequestDTO;
import model.home.BudgetResponseDTO;
import model.response.ServiceResponse;
import security.SecurityUtils;
import service.BudgetService;

import java.util.List;

@Path("/budgets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class BudgetResource {

    @Inject
    BudgetService svc;
    @Inject
    SecurityUtils sec;

    @GET
    public Response list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size
    ) {
        ServiceResponse<PagedResponseDTO<BudgetResponseDTO>> resp =
                svc.list(sec.getCurrentUser(), page, size);
        return Response.status(resp.getStatusCode()).entity(resp).build();
    }

    @POST
    @Path("/bulk")
    public Response createBulk(@Valid List<BudgetRequestDTO> dtos) {
        ServiceResponse<List<BudgetResponseDTO>> resp =
                svc.bulkCreate(sec.getCurrentUser(), dtos);
        return Response.status(resp.getStatusCode()).entity(resp).build();
    }

    @PATCH
    @Path("/bulk")
    public Response patchBulk(@Valid List<BudgetRequestDTO> dtos) {
        ServiceResponse<List<BudgetResponseDTO>> resp =
                svc.bulkPatch(sec.getCurrentUser(), dtos);
        return Response.status(resp.getStatusCode()).entity(resp).build();
    }

    @DELETE
    public Response deleteBulk(@Valid List<Long> categoryIds) {
        var resp = svc.deleteBulk(sec.getCurrentUser(), categoryIds);
        return Response.status(resp.getStatusCode()).entity(resp).build();
    }
}
