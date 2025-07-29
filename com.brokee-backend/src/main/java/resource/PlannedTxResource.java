package resource;

import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import model.response.ServiceResponseDTO;
import model.response.ServiceResponseDirector;
import model.transaction.PlannedTxRequestDTO;
import model.transaction.PlannedTxResponseDTO;
import security.SecurityUtils;
import service.PlannedTxService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Path("/api/planned-transactions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class PlannedTxResource {

    @Inject
    PlannedTxService service;

    @Inject
    SecurityUtils securityUtils;

    @GET
    public Response list(
            @QueryParam("title") String title,
            @QueryParam("dueFrom") String dueFromStr,
            @QueryParam("dueTo") String dueToStr
    ) {
        LocalDate dueFrom = null, dueTo = null;
        try {
            if (dueFromStr != null) dueFrom = LocalDate.parse(dueFromStr);
            if (dueToStr != null) dueTo = LocalDate.parse(dueToStr);
        } catch (DateTimeParseException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ServiceResponseDirector.errorBadRequest("Invalid date format"))
                    .build();
        }

        ServiceResponseDTO<List<PlannedTxResponseDTO>> resp =
                service.list(securityUtils.getCurrentUser(), title, dueFrom, dueTo);

        return Response.status(resp.getStatusCode())
                .entity(resp)
                .build();
    }

    @GET
    @Path("{id}")
    public Response getOne(@PathParam("id") Long id) {
        ServiceResponseDTO<PlannedTxResponseDTO> resp = service.getById(securityUtils.getCurrentUser(), id);
        return Response.status(resp.getStatusCode())
                .entity(resp)
                .build();
    }

    @POST
    public Response create(@Valid PlannedTxRequestDTO dto, @Context UriInfo uriInfo) {
        ServiceResponseDTO<PlannedTxResponseDTO> resp = service.create(securityUtils.getCurrentUser(), dto);
        return Response.status(resp.getStatusCode()).entity(resp).build();
    }

    @PUT
    @Path("{id}")
    public Response update(
            @PathParam("id") Long id,
            @Valid PlannedTxRequestDTO dto
    ) {
        ServiceResponseDTO<PlannedTxResponseDTO> resp =
                service.update(securityUtils.getCurrentUser(), id, dto);

        return Response.status(resp.getStatusCode())
                .entity(resp)
                .build();
    }

    @DELETE
    @Path("{id}")
    public Response deleteOne(@PathParam("id") Long id) {
        ServiceResponseDTO<Boolean> resp = service.delete(securityUtils.getCurrentUser(), id);
        return Response.status(resp.getStatusCode())
                .entity(resp)
                .build();
    }
}
