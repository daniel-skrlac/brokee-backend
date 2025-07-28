package resource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import model.notification.RegisterPushDTO;
import security.SecurityUtils;
import service.NotificationRegistrationService;

@Path("/api/notifications")
@ApplicationScoped
public class NotificationRegistrationResource {

    @Inject
    NotificationRegistrationService registrationService;

    @Inject
    SecurityUtils securityUtils;

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(@Valid RegisterPushDTO dto) {
        registrationService.registerPlayerId(securityUtils.getCurrentUser(), dto.playerId());
        return Response.ok().build();
    }

    @DELETE
    @Path("/deregister")
    public Response deregister() {
        registrationService.deregisterPlayerId(securityUtils.getCurrentUser());
        return Response.noContent().build();
    }
}
