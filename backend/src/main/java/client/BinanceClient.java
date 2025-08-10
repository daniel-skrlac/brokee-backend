package client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import model.external.AccountSummaryDTO;
import model.external.BinanceAccountDTO;
import model.external.TickerPriceDTO;
import model.external.TradeDTO;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@Path("/api/v3")
@RegisterRestClient(configKey = "binance-api")
@Produces("application/json")
@Consumes("application/json")
public interface BinanceClient {
    @GET
    @Path("/ticker/24hr")
    List<TickerPriceDTO> get24hrTicker();

    @GET
    @Path("/account")
    BinanceAccountDTO getAccount(
            @HeaderParam("X-MBX-APIKEY") String apiKey,
            @QueryParam("recvWindow") long recvWindow,
            @QueryParam("timestamp") long timestamp,
            @QueryParam("signature") String signature
    );

    @GET
    @Path("/myTrades")
    @Consumes(MediaType.APPLICATION_JSON)
    List<TradeDTO> getMyTrades(
            @HeaderParam("X-MBX-APIKEY") String apiKey,
            @QueryParam("symbol") String symbol,
            @QueryParam("limit") int limit,
            @QueryParam("recvWindow") long recvWindow,
            @QueryParam("timestamp") long timestamp,
            @QueryParam("signature") String signature
    );
}
