package resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import model.response.ServiceResponseDTO;
import model.tracking.DashboardSummaryDTO;
import security.SecurityUtils;
import service.DashboardService;

@Path("/api/dashboard")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class DashboardResource {

    @Inject
    DashboardService dashService;

    @Inject
    SecurityUtils securityUtils;

    @GET
    @Path("/summary")
    public Response summary() {
        ServiceResponseDTO<DashboardSummaryDTO> resp =
                dashService.summary(securityUtils.getCurrentUser());
        return Response.status(resp.getStatusCode()).entity(resp).build();
    }
}
