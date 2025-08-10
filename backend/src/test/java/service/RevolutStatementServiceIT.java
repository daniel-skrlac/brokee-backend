package service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class RevolutStatementServiceIT {

    @Inject
    RevolutStatementService svc;

    @Test
    void parseMonthlyStatement_parses_lines() throws Exception {
        byte[] pdf = createPdf("""
                    12.05.2024 Some Coffee Shop €-4,50 €0,00
                    01. sij 2024. Salary €0,00 €1.000,00
                """);

        var lines = svc.parseMonthlyStatement(new ByteArrayInputStream(pdf));
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).description()).contains("Coffee");
        assertThat(lines.get(0).sentAmount()).isEqualByComparingTo(new BigDecimal("-4.50"));
        assertThat(lines.get(1).receivedAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    private static byte[] createPdf(String text) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.setFont(PDType1Font.HELVETICA, 12);
                float y = 750;
                for (String line : text.split("\\R")) {
                    cs.beginText();
                    cs.newLineAtOffset(50, y);
                    cs.showText(line);
                    cs.endText();
                    y -= 18;
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }
}
