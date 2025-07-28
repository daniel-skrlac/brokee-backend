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
import model.entity.BinanceToken;
import model.external.BinanceCredentialDTO;
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
    public ServiceResponseDTO<BinanceToken> upsertCredentials(BinanceCredentialDTO dto) {
        return service.saveCredentials(securityUtils.getCurrentUser(), dto.apiKey(), dto.secretKey());
    }

    @GET
    public ServiceResponseDTO<BinanceToken> getCredentials() {
        return service.getCredentials(securityUtils.getCurrentUser());
    }

    @GET
    @Path("/portfolio")
    public ServiceResponseDTO<FullPortfolioDTO> getPortfolio(@QueryParam("currency") @DefaultValue("EUR") String currency) {
        return service.getPortfolio(securityUtils.getCurrentUser(), currency);
    }

    @DELETE
    public ServiceResponseDTO<Boolean> deleteCredentials() {
        return service.deleteCredentials(securityUtils.getCurrentUser());
    }
}
