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
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import model.helper.PagedResponseDTO;
import model.home.FullTxRequestDTO;
import model.home.QuickTxRequestDTO;
import model.home.TxResponseDTO;
import model.response.ServiceResponseDTO;
import model.tracking.CategoryBreakdownDTO;
import model.tracking.LocationDTO;
import model.tracking.SpendingVsIncomeDTO;
import security.SecurityUtils;
import service.TransactionService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Path("/api/transactions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class TransactionResource {

    @Inject
    TransactionService txService;
    @Inject
    SecurityUtils securityUtils;


    @GET
    @Path("/recent")
    public Response recent(@QueryParam("limit") @DefaultValue("5") int limit) {
        ServiceResponseDTO<List<TxResponseDTO>> resp =
                txService.recent(securityUtils.getCurrentUser(), limit);
        return Response.status(resp.getStatusCode()).entity(resp).build();
    }

    @GET
    public Response page(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("type") String type,
            @QueryParam("min") BigDecimal minAmount,
            @QueryParam("max") BigDecimal maxAmount,
            @QueryParam("dueFrom") String dueFromStr,
            @QueryParam("dueTo") String dueToStr,
            @QueryParam("note") String note,
            @QueryParam("category") String categoryName
    ) {
        ServiceResponseDTO<PagedResponseDTO<TxResponseDTO>> resp =
                txService.page(
                        securityUtils.getCurrentUser(),
                        page, size,
                        type, minAmount, maxAmount,
                        dueFromStr, dueToStr,
                        note,
                        categoryName     // ← pass along
                );
        return Response
                .status(resp.getStatusCode())
                .entity(resp)
                .build();
    }


    @GET
    @Path("/balance")
    public Response balance() {
        ServiceResponseDTO<BigDecimal> resp =
                txService.getBalance(securityUtils.getCurrentUser());
        return Response.status(resp.getStatusCode()).entity(resp).build();
    }

    @GET
    @Path("/range")
    public Response byDateRange(
            @QueryParam("from") String from,
            @QueryParam("to") String to
    ) {
        OffsetDateTime f = OffsetDateTime.parse(from);
        OffsetDateTime t = OffsetDateTime.parse(to);
        ServiceResponseDTO<List<TxResponseDTO>> resp =
                txService.findByDateRange(securityUtils.getCurrentUser(), f, t);
        return Response.status(resp.getStatusCode()).entity(resp).build();
    }

    @GET
    @Path("/graph/daily")
    public Response dailyGraph(
            @QueryParam("days") @DefaultValue("7") int days
    ) {
        ServiceResponseDTO<Map<String, BigDecimal>> resp =
                txService.findDailyExpenses(securityUtils.getCurrentUser(), days);
        return Response.status(resp.getStatusCode()).entity(resp).build();
    }

    @GET
    @Path("/graph/monthly")
    public Response monthlyGraph(
            @QueryParam("year") Integer year
    ) {
        if (year == null) {
            year = java.time.LocalDate.now().getYear();
        }
        ServiceResponseDTO<Map<String, BigDecimal>> resp =
                txService.findMonthlyExpenses(securityUtils.getCurrentUser(), year);
        return Response.status(resp.getStatusCode()).entity(resp).build();
    }

    @GET
    @Path("/spending-vs-income")
    public Response spendingVsIncome(@QueryParam("year") @DefaultValue("2025") int year) {
        ServiceResponseDTO<List<SpendingVsIncomeDTO>> resp =
                txService.spendingVsIncome(securityUtils.getCurrentUser(), year);
        return Response.status(resp.getStatusCode()).entity(resp).build();
    }

    @GET
    @Path("/category-breakdown")
    public Response categoryBreakdown(@QueryParam("month") String monthKey) {
        ServiceResponseDTO<List<CategoryBreakdownDTO>> resp =
                txService.categoryBreakdown(securityUtils.getCurrentUser(), monthKey);
        return Response.status(resp.getStatusCode()).entity(resp).build();
    }

    @GET
    @Path("/top-locations")
    public Response topLocations(@QueryParam("limit") @DefaultValue("3") int limit) {
        ServiceResponseDTO<List<LocationDTO>> resp =
                txService.topLocations(securityUtils.getCurrentUser(), limit);
        return Response.status(resp.getStatusCode()).entity(resp).build();
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") Long id) {
        ServiceResponseDTO<TxResponseDTO> resp =
                txService.findById(securityUtils.getCurrentUser(), id);
        return Response.status(resp.getStatusCode()).entity(resp).build();
    }

    @POST
    @Path("/quick")
    public Response quickAdd(@Valid QuickTxRequestDTO dto) {
        ServiceResponseDTO<TxResponseDTO> resp =
                txService.quickAdd(securityUtils.getCurrentUser(), dto);
        return Response.status(resp.getStatusCode()).entity(resp).build();
    }

    @POST
    public Response create(@Valid FullTxRequestDTO dto) {
        ServiceResponseDTO<TxResponseDTO> resp =
                txService.create(securityUtils.getCurrentUser(), dto);
        return Response.status(resp.getStatusCode()).entity(resp).build();
    }

    @PATCH
    @Path("/{id}")
    public Response patch(
            @PathParam("id") Long id,
            @Valid FullTxRequestDTO dto
    ) {
        ServiceResponseDTO<TxResponseDTO> resp =
                txService.update(securityUtils.getCurrentUser(), id, dto);
        return Response.status(resp.getStatusCode()).entity(resp).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        ServiceResponseDTO<Boolean> resp =
                txService.delete(securityUtils.getCurrentUser(), id);
        return Response.status(resp.getStatusCode()).entity(resp).build();
    }
}
