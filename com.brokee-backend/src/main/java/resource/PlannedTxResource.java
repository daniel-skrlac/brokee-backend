package resource;

import io.quarkus.security.Authenticated;
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
import model.helper.PagedResponseDTO;
import model.response.ServiceResponseDTO;
import model.response.ServiceResponseDirector;
import model.transaction.PlannedTxRequestDTO;
import model.transaction.PlannedTxResponseDTO;
import security.SecurityUtils;
import service.PlannedTxService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

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
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size,
            @QueryParam("title") String title,
            @QueryParam("dueFrom") String dueFromStr,
            @QueryParam("dueTo") String dueToStr,
            @QueryParam("type") String type,
            @QueryParam("min") Double minAmount,
            @QueryParam("max") Double maxAmount,
            @QueryParam("category") String categoryName
    ) {
        LocalDate dueFrom = null, dueTo = null;
        try {
            if (dueFromStr != null) dueFrom = LocalDate.parse(dueFromStr);
            if (dueToStr != null) dueTo = LocalDate.parse(dueToStr);
        } catch (DateTimeParseException ex) {
            return Response.status(BAD_REQUEST)
                    .entity(ServiceResponseDirector.errorBadRequest("Invalid date format"))
                    .build();
        }

        boolean doPaging = (page != null && size != null);
        if (doPaging) {
            ServiceResponseDTO<PagedResponseDTO<PlannedTxResponseDTO>> resp =
                    service.page(
                            securityUtils.getCurrentUser(),
                            page, size,
                            title, dueFrom, dueTo,
                            type, minAmount, maxAmount,
                            categoryName
                    );
            return Response.status(resp.getStatusCode())
                    .entity(resp)
                    .build();
        } else {
            ServiceResponseDTO<List<PlannedTxResponseDTO>> resp =
                    service.list(
                            securityUtils.getCurrentUser(),
                            title, dueFrom, dueTo,
                            type, minAmount, maxAmount,
                            categoryName
                    );
            return Response.status(resp.getStatusCode())
                    .entity(resp)
                    .build();
        }
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
    public Response create(@Valid PlannedTxRequestDTO dto) {
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
