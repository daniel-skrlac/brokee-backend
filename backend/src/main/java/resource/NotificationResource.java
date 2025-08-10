package resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import model.notification.DeregisterDTO;
import model.notification.RegisterDTO;
import security.SecurityUtils;
import service.NotificationService;

@Path("/api/notifications")
@Authenticated
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class NotificationResource {

    @Inject
    SecurityUtils security;

    @Inject
    NotificationService notifications;

    @POST
    @Path("/register")
    @Transactional
    public Response register(RegisterDTO dto) {
        final String subject = security.getCurrentUser();
        if (subject == null || dto == null || dto.subscriptionId() == null || dto.subscriptionId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Missing subject or subscriptionId").build();
        }

        notifications.registerSubscriptionForUser(subject, dto.subscriptionId());

        return Response.noContent().build();
    }

    @POST
    @Path("/deregister")
    @Transactional
    public Response deregister(DeregisterDTO dto) {
        String subject = security.getCurrentUser();
        if (subject == null) return Response.status(Response.Status.UNAUTHORIZED).build();

        if (dto != null && dto.subscriptionId() != null && !dto.subscriptionId().isBlank()) {
            notifications.deregisterBySubscriptionId(dto.subscriptionId());
        } else {
            notifications.deregisterForUser(subject);
        }
        return Response.noContent().build();
    }
}
