package service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import model.response.ServiceResponseDTO;
import org.junit.jupiter.api.Test;
import repository.TransactionRepository;
import utils.NoDbProfile;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(NoDbProfile.class)
class RevolutImportServiceTest {

    @Inject
    RevolutImportService svc;
    @InjectMock
    RevolutStatementService parser;
    @InjectMock
    CategoryService categoryService;
    @InjectMock
    TransactionRepository txRepo;

    @Test
    void importMonthlyPdf_returnsBadRequest_onParseFailure() throws Exception {
        when(parser.parseMonthlyStatement(any())).thenThrow(new RuntimeException("bad pdf"));

        ServiceResponseDTO<?> res = svc.importMonthlyPdf("u", new ByteArrayInputStream(new byte[0]));
        assertThat(res.isSuccess()).isFalse();
    }
}
