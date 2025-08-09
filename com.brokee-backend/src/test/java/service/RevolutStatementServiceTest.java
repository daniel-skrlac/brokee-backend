package service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import model.external.RevolutTransactionDTO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import repository.BinanceTokenRepository;
import repository.BudgetRepository;
import repository.CategoryRepository;
import repository.TransactionRepository;
import repository.UserPushTokenRepository;
import utils.NoDbProfile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@TestProfile(NoDbProfile.class)
class RevolutStatementServiceTest {

    @Inject
    RevolutStatementService svc;

    @InjectMock
    TransactionRepository txRepo;
    @InjectMock
    UserPushTokenRepository userPushTokenRepository;
    @InjectMock
    BinanceTokenRepository binanceTokenRepository;
    @InjectMock
    BudgetRepository budgetRepository;
    @InjectMock
    CategoryRepository categoryRepository;

    private byte[] pdfWithLines(String... lines) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.setFont(PDType1Font.HELVETICA, 10);
                float y = 800;
                for (String l : lines) {
                    cs.beginText();
                    cs.newLineAtOffset(50, y);
                    cs.showText(l);
                    cs.endText();
                    y -= 14;
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    @Test
    void parseMonthlyStatement_MixedDateFormatsAndAmounts_ParsesTwoRows() throws Exception {
        String l1 = "01.08.2025  Some Shop            €-123,45   €0,00";
        String l2 = "2. kol 2025.  Salary            €0,00      €3.210,99";

        byte[] pdf = pdfWithLines(l1, l2);
        List<RevolutTransactionDTO> rows = svc.parseMonthlyStatement(new ByteArrayInputStream(pdf));

        assertThat(rows).hasSize(2);

        var r1 = rows.get(0);
        assertThat(r1.date()).isEqualTo(LocalDate.of(2025, 8, 1));
        assertThat(r1.sentAmount()).isEqualByComparingTo(new BigDecimal("-123.45"));
        assertThat(r1.receivedAmount()).isEqualByComparingTo(new BigDecimal("0.00"));

        var r2 = rows.get(1);
        assertThat(r2.date()).isEqualTo(LocalDate.of(2025, 8, 2));
        assertThat(r2.sentAmount()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(r2.receivedAmount()).isEqualByComparingTo(new BigDecimal("3210.99"));
    }

    @Test
    void parseMonthlyStatement_AsciiMinusAndThousands_ParsesNegative() throws Exception {
        String l = "03.08.2025  Big Purchase         €-1.234,56   €0,00";

        byte[] pdf = pdfWithLines(l);
        List<RevolutTransactionDTO> rows = svc.parseMonthlyStatement(new ByteArrayInputStream(pdf));

        assertThat(rows).hasSize(1);
        var r = rows.get(0);
        assertThat(r.date()).isEqualTo(LocalDate.of(2025, 8, 3));
        assertThat(r.sentAmount()).isEqualByComparingTo(new BigDecimal("-1234.56"));
        assertThat(r.receivedAmount()).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    void parseMonthlyStatement_LargeAmountWithMultipleThousands_Parsed() throws Exception {
        String l = "06.08.2025  Big Transfer         €-9.999.999,99   €0,00";

        byte[] pdf = pdfWithLines(l);
        var rows = svc.parseMonthlyStatement(new ByteArrayInputStream(pdf));

        assertThat(rows).hasSize(1);
        var r = rows.get(0);
        assertThat(r.date()).isEqualTo(LocalDate.of(2025, 8, 6));
        assertThat(r.sentAmount()).isEqualByComparingTo(new BigDecimal("-9999999.99"));
        assertThat(r.receivedAmount()).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    void parseMonthlyStatement_LeapDayNumericDate_Parsed() throws Exception {
        String l = "29.02.2024  Salary               €0,00      €123,45";

        byte[] pdf = pdfWithLines(l);
        var rows = svc.parseMonthlyStatement(new ByteArrayInputStream(pdf));

        assertThat(rows).hasSize(1);
        var r = rows.get(0);
        assertThat(r.date()).isEqualTo(LocalDate.of(2024, 2, 29));
        assertThat(r.sentAmount()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(r.receivedAmount()).isEqualByComparingTo(new BigDecimal("123.45"));
    }

    @Test
    void parseMonthlyStatement_NonEuroCurrency_Skipped() throws Exception {
        String l1 = "07.08.2025  USD purchase         $-10.00    $0.00";
        String l2 = "07.08.2025  Normal EUR           €-1,00     €0,00";

        byte[] pdf = pdfWithLines(l1, l2);
        var rows = svc.parseMonthlyStatement(new ByteArrayInputStream(pdf));

        assertThat(rows).hasSize(1);
        var r = rows.get(0);
        assertThat(r.date()).isEqualTo(LocalDate.of(2025, 8, 7));
        assertThat(r.sentAmount()).isEqualByComparingTo(new BigDecimal("-1.00"));
        assertThat(r.receivedAmount()).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    void parseMonthlyStatement_EmptyPdf_ReturnsEmpty() throws Exception {
        byte[] pdf = pdfWithLines(); // no lines
        var rows = svc.parseMonthlyStatement(new ByteArrayInputStream(pdf));
        assertThat(rows).isEmpty();
    }

    @Test
    void parseMonthlyStatement_IrregularSpacing_Parsed() throws Exception {
        String l = "08.08.2025     Groceries         €-123,45            €0,00";

        byte[] pdf = pdfWithLines(l);
        var rows = svc.parseMonthlyStatement(new ByteArrayInputStream(pdf));

        assertThat(rows).hasSize(1);
        var r = rows.get(0);
        assertThat(r.date()).isEqualTo(LocalDate.of(2025, 8, 8));
        assertThat(r.sentAmount()).isEqualByComparingTo(new BigDecimal("-123.45"));
        assertThat(r.receivedAmount()).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    void parseMonthlyStatement_HeaderAndTotals_Skipped() throws Exception {
        String header = "Statement for August 2025";
        String total = "Total for period: €1.234,56";
        String tx = "09.08.2025  Coffee             €-2,50      €0,00";

        byte[] pdf = pdfWithLines(header, total, tx);
        var rows = svc.parseMonthlyStatement(new ByteArrayInputStream(pdf));

        assertThat(rows).hasSize(1);
        var r = rows.get(0);
        assertThat(r.date()).isEqualTo(LocalDate.of(2025, 8, 9));
        assertThat(r.sentAmount()).isEqualByComparingTo(new BigDecimal("-2.50"));
        assertThat(r.receivedAmount()).isEqualByComparingTo(new BigDecimal("0.00"));
    }
}
