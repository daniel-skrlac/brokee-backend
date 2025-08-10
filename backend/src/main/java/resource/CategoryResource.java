package resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import model.home.CategoryResponseDTO;
import model.response.ServiceResponseDTO;
import service.CategoryService;

import java.util.List;

@Path("/api/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class CategoryResource {

    @Inject CategoryService svc;

    @GET
    public Response getAll(@QueryParam("name") String name) {
        ServiceResponseDTO<List<CategoryResponseDTO>> resp =
                (name == null || name.isBlank())
                        ? svc.listAll()
                        : svc.search(name);
        return Response
                .status(resp.getStatusCode())
                .entity(resp)
                .build();
    }
}
