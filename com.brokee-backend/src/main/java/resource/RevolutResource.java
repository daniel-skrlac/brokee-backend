package resource;

import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import mapper.TransactionMapper;
import model.home.TxResponseDTO;
import model.response.ServiceResponseDirector;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import security.SecurityUtils;
import service.RevolutImportService;
import service.TransactionService;

import java.io.InputStream;
import java.util.List;

@Path("/api/revolut")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.MULTIPART_FORM_DATA)
@Authenticated
public class RevolutResource {

    @Inject
    RevolutImportService importSvc;
    @Inject
    TransactionService txService;
    @Inject
    SecurityUtils securityUtils;
    @Inject
    TransactionMapper transactionMapper;

    @POST
    @Path("/import")
    @Blocking
    public Response importPdf(@RestForm("file") FileUpload file) {
        try (InputStream in = file.uploadedFile().toFile().toPath().toUri().toURL().openStream()) {
            var resp = importSvc.importMonthlyPdf(securityUtils.getCurrentUser(), in);

            List<TxResponseDTO> dtos = ((List<?>) resp.getData()).stream()
                    .map(o -> (model.entity.Transaction) o)
                    .map(transactionMapper::entityToResponse)
                    .toList();

            var out = ServiceResponseDirector.successCreated(dtos, resp.getMessage());
            return Response.status(out.getStatusCode()).entity(out).build();
        } catch (Exception e) {
            var err = ServiceResponseDirector.errorBadRequest("Upload failed");
            return Response.status(err.getStatusCode()).entity(err).build();
        }
    }
}
