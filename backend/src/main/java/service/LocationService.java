package service;

import client.GeocodeClient;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import model.response.client.ReverseGeocodeResponse;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class LocationService {

    @Inject
    @RestClient
    GeocodeClient geocode;

    @Inject
    @ConfigProperty(name = "geocode.api-key")
    String apiKey;

    @CacheResult(cacheName = "location-service")
    public String getLocationName(double lat, double lon) {
        ReverseGeocodeResponse resp = geocode.reverse(lat, lon, apiKey);
        return resp.display_name() != null
                ? resp.display_name()
                : String.format("%.6f,%.6f", lat, lon);
    }
}
