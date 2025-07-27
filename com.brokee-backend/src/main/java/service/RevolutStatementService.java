// src/main/java/service/RevolutStatementService.java
package service;

import jakarta.enterprise.context.ApplicationScoped;
import model.external.RevolutTransactionDTO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class RevolutStatementService {
    // adjust pattern to your PDF’s date format + columns
    private static final Pattern LINE_PATTERN = Pattern.compile(
            "(\\d{2}\\.\\d{2}\\.\\d{4})\\s+(.+?)\\s+([\\d,\\.]+)\\s+([\\d,\\.]+)"
    );
    private static final DateTimeFormatter DF =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public List<RevolutTransactionDTO> parseMonthlyStatement(InputStream pdfStream) throws Exception {
        try (PDDocument doc = PDDocument.load(pdfStream)) {
            PDFTextStripper strip = new PDFTextStripper();
            String text = strip.getText(doc);
            List<RevolutTransactionDTO> list = new ArrayList<>();

            for (String line : text.split("\\r?\\n")) {
                Matcher m = LINE_PATTERN.matcher(line.trim());
                if (!m.matches()) continue;
                LocalDate date = LocalDate.parse(m.group(1), DF);
                String desc = m.group(2).trim();
                BigDecimal sent = new BigDecimal(m.group(3).replace(",", ""));
                BigDecimal rec = new BigDecimal(m.group(4).replace(",", ""));
                list.add(new RevolutTransactionDTO(date, desc, sent, rec));
            }
            return list;
        }
    }
}
