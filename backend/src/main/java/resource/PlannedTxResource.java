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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import model.response.ServiceResponseDTO;
import model.response.ServiceResponseDirector;
import model.transaction.PlannedTxRequestDTO;
import model.transaction.PlannedTxResponseDTO;
import security.SecurityUtils;
import service.PlannedTxService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

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
            if (dueFromStr != null && !dueFromStr.isBlank()) dueFrom = LocalDate.parse(dueFromStr);
            if (dueToStr != null && !dueToStr.isBlank()) dueTo = LocalDate.parse(dueToStr);
        } catch (DateTimeParseException ex) {
            return Response.status(BAD_REQUEST)
                    .entity(ServiceResponseDirector.errorBadRequest("Invalid date format"))
                    .build();
        }

        boolean paginationRequested = (page != null) || (size != null);
        String type1 = (type != null && !type.isBlank()) ? type : null;
        String name = (categoryName != null && !categoryName.isBlank()) ? categoryName : null;
        if (paginationRequested) {
            if (size == null || size <= 0) {
                return Response.status(BAD_REQUEST)
                        .entity(ServiceResponseDirector.errorBadRequest("Page size must be greater than 0"))
                        .build();
            }
            if (page == null || page < 0) page = 0;

            var resp = service.page(
                    securityUtils.getCurrentUser(),
                    page, size,
                    title, dueFrom, dueTo,
                    type1,
                    minAmount, maxAmount,
                    name
            );
            return Response.status(resp.getStatusCode()).entity(resp).build();
        }

        var resp = service.list(
                securityUtils.getCurrentUser(),
                title, dueFrom, dueTo,
                type1,
                minAmount, maxAmount,
                name
        );
        return Response.status(resp.getStatusCode()).entity(resp).build();
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
