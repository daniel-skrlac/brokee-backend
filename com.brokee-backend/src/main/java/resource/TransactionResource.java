package resource;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import model.helper.PagedResponseDTO;
import model.home.FullTxRequestDTO;
import model.home.QuickTxRequestDTO;
import model.home.TxResponseDTO;
import model.response.ServiceResponse;
import service.TransactionService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Path("/transactions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class TransactionResource {

    @Inject TransactionService txService;
    @Inject SecurityIdentity   identity;

    private String currentUser() {
        return identity.getPrincipal().getName();
    }

    @GET @Path("/recent")
    public Response recent(@QueryParam("limit") @DefaultValue("5") int limit) {
        ServiceResponse<List<TxResponseDTO>> resp =
                txService.recent(currentUser(), limit);
        return Response.status(resp.getStatusCode()).entity(resp).build();
    }

    @GET
    public Response page(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size
    ) {
        ServiceResponse<PagedResponseDTO<TxResponseDTO>> resp =
                txService.page(currentUser(), page, size);
        return Response.status(resp.getStatusCode()).entity(resp).build();
    }

    @GET @Path("/balance")
    public Response balance() {
        ServiceResponse<BigDecimal> resp =
                txService.getBalance(currentUser());
        return Response.status(resp.getStatusCode()).entity(resp).build();
    }

    @GET @Path("/range")
    public Response byDateRange(
            @QueryParam("from") String from,
            @QueryParam("to")   String to
    ) {
        OffsetDateTime f = OffsetDateTime.parse(from);
        OffsetDateTime t = OffsetDateTime.parse(to);
        ServiceResponse<List<TxResponseDTO>> resp =
                txService.findByDateRange(currentUser(), f, t);
        return Response.status(resp.getStatusCode()).entity(resp).build();
    }

    @GET @Path("/graph/daily")
    public Response dailyGraph(
            @QueryParam("days") @DefaultValue("7") int days
    ) {
        ServiceResponse<Map<String, BigDecimal>> resp =
                txService.findDailyExpenses(currentUser(), days);
        return Response.status(resp.getStatusCode()).entity(resp).build();
    }

    @GET @Path("/graph/monthly")
    public Response monthlyGraph(
            @QueryParam("year") @DefaultValue("#{T(java.time.LocalDate).now().year}") int year
    ) {
        ServiceResponse<Map<String, BigDecimal>> resp =
                txService.findMonthlyExpenses(currentUser(), year);
        return Response.status(resp.getStatusCode()).entity(resp).build();
    }

    @GET @Path("/{id}")
    public Response getById(@PathParam("id") Long id) {
        ServiceResponse<TxResponseDTO> resp =
                txService.findById(currentUser(), id);
        return Response.status(resp.getStatusCode()).entity(resp).build();
    }

    @POST @Path("/quick")
    public Response quickAdd(@Valid QuickTxRequestDTO dto) {
        ServiceResponse<TxResponseDTO> resp =
                txService.quickAdd(currentUser(), dto);
        return Response.status(resp.getStatusCode()).entity(resp).build();
    }

    @POST
    public Response create(@Valid FullTxRequestDTO dto) {
        ServiceResponse<TxResponseDTO> resp =
                txService.create(currentUser(), dto);
        return Response.status(resp.getStatusCode()).entity(resp).build();
    }

    @PATCH @Path("/{id}")
    public Response patch(
            @PathParam("id") Long id,
            @Valid FullTxRequestDTO dto
    ) {
        ServiceResponse<TxResponseDTO> resp =
                txService.update(currentUser(), id, dto);
        return Response.status(resp.getStatusCode()).entity(resp).build();
    }

    @DELETE @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        ServiceResponse<Boolean> resp =
                txService.delete(currentUser(), id);
        return Response.status(resp.getStatusCode()).entity(resp).build();
    }
}
