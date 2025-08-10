package service;

import client.GeocodeClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import model.response.client.ReverseGeocodeResponse;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import utils.NoDbProfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(NoDbProfile.class)
class LocationServiceTest {

    @InjectMock
    @RestClient
    GeocodeClient geocode;

    @Inject
    LocationService svc;

    @Test
    void getLocationName_DisplayNamePresent_ReturnsDisplayName() {
        var resp = mock(ReverseGeocodeResponse.class);
        when(resp.display_name()).thenReturn("Main Square");
        when(geocode.reverse(anyDouble(), anyDouble(), anyString())).thenReturn(resp);

        assertThat(svc.getLocationName(1.0, 2.0)).isEqualTo("Main Square");
        verify(geocode).reverse(anyDouble(), anyDouble(), anyString());
    }

    @Test
    void getLocationName_DisplayNameNull_FallsBackToCoordinates() {
        var resp = mock(ReverseGeocodeResponse.class);
        when(resp.display_name()).thenReturn(null);
        when(geocode.reverse(anyDouble(), anyDouble(), anyString())).thenReturn(resp);

        assertThat(svc.getLocationName(3.0, 4.0)).isEqualTo("3.000000,4.000000");
        verify(geocode).reverse(anyDouble(), anyDouble(), anyString());
    }
}
