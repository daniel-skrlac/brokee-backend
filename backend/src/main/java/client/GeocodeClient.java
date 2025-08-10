package client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import model.response.client.ReverseGeocodeResponse;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "geocode-api")
public interface GeocodeClient {
    @GET
    @Path("/reverse")
    @Produces(MediaType.APPLICATION_JSON)
    ReverseGeocodeResponse reverse(
            @QueryParam("lat") double lat,
            @QueryParam("lon") double lon,
            @QueryParam("api_key") String apiKey
    );
}
