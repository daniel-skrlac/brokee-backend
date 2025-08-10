package resource;

import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import model.external.BinanceCredentialDTO;
import model.external.BinanceTokenDTO;
import model.external.FullPortfolioDTO;
import model.response.ServiceResponseDTO;
import security.SecurityUtils;
import service.BinanceService;

@Path("/api/binance")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class BinanceResource {

    @Inject
    BinanceService service;

    @Inject
    SecurityUtils securityUtils;

    @POST
    public Response upsertCredentials(BinanceCredentialDTO dto) {
        ServiceResponseDTO<BinanceTokenDTO> result = service.saveCredentials(
                securityUtils.getCurrentUser(),
                dto.apiKey(),
                dto.secretKey()
        );
        return Response.status(result.getStatusCode())
                .entity(result)
                .build();
    }

    @GET
    public Response getCredentials() {
        ServiceResponseDTO<BinanceTokenDTO> result = service.getCredentials(
                securityUtils.getCurrentUser()
        );
        return Response.status(result.getStatusCode())
                .entity(result)
                .build();
    }

    @GET
    @Path("/portfolio")
    public Response getPortfolio(@QueryParam("currency") @DefaultValue("EUR") String currency) {
        ServiceResponseDTO<FullPortfolioDTO> result = service.getPortfolio(
                securityUtils.getCurrentUser(),
                currency
        );
        return Response.status(result.getStatusCode())
                .entity(result)
                .build();
    }

    @DELETE
    public Response deleteCredentials() {
        ServiceResponseDTO<BinanceTokenDTO> result = service.deleteCredentials(
                securityUtils.getCurrentUser()
        );
        return Response.status(result.getStatusCode())
                .entity(result)
                .build();
    }
}
