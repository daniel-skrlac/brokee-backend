package resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import model.response.ServiceResponse;
import model.settings.SavingsGoalRequestDTO;
import model.settings.SavingsGoalResponseDTO;
import security.SecurityUtils;
import service.SavingsGoalService;

@Path("/savings")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SavingsGoalResource {
    @Inject
    SavingsGoalService svc;
    @Inject
    SecurityUtils sec;

    @GET
    public Response get() {
        ServiceResponse<SavingsGoalResponseDTO> r =
                svc.get(sec.getCurrentUser());
        return Response.status(r.getStatusCode()).entity(r).build();
    }

    @POST
    public Response upsert(@Valid SavingsGoalRequestDTO dto) {
        ServiceResponse<SavingsGoalResponseDTO> r =
                svc.upsert(sec.getCurrentUser(), dto);
        return Response.status(r.getStatusCode()).entity(r).build();
    }

    @PATCH
    public Response patch(@Valid SavingsGoalRequestDTO dto) {
        ServiceResponse<SavingsGoalResponseDTO> r =
                svc.patch(sec.getCurrentUser(), dto);
        return Response.status(r.getStatusCode()).entity(r).build();
    }

    @DELETE
    public Response delete() {
        ServiceResponse<Boolean> r =
                svc.delete(sec.getCurrentUser());
        return Response.status(r.getStatusCode()).entity(r).build();
    }
}
