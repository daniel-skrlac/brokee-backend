package service;

import jakarta.enterprise.context.ApplicationScoped;
import model.external.RevolutTransactionDTO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class RevolutStatementService {

    private static final Pattern LINE = Pattern.compile(
            "^\\s*" +
                    "(\\d{2}\\.\\d{2}\\.\\d{4}|\\d{1,2}\\.\\s*[a-zšđčćž]{3}\\s+\\d{4}\\.)" +
                    "\\s+" +
                    "(.+?)" +
                    "\\s+€?([-−]?\\d[\\d.,]*)" +
                    "\\s+€?([-−]?\\d[\\d.,]*)\\s*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final DateTimeFormatter NUM_DF = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static final Map<String, Integer> HR_MONTH = Map.ofEntries(
            Map.entry("sij", 1), Map.entry("vel", 2), Map.entry("ožu", 3),
            Map.entry("tra", 4), Map.entry("svi", 5), Map.entry("lip", 6),
            Map.entry("srp", 7), Map.entry("kol", 8), Map.entry("ruj", 9),
            Map.entry("lis", 10), Map.entry("stu", 11), Map.entry("pro", 12)
    );

    public List<RevolutTransactionDTO> parseMonthlyStatement(InputStream pdf) throws IOException {
        List<RevolutTransactionDTO> out = new ArrayList<>();

        try (PDDocument doc = PDDocument.load(pdf)) {
            String text = new PDFTextStripper().getText(doc);

            for (String raw : text.split("\\R")) {
                Matcher m = LINE.matcher(raw.trim());
                if (!m.matches()) continue;

                LocalDate date = parseDate(m.group(1));
                String desc = m.group(2).trim();
                BigDecimal sent = toDecimal(m.group(3));
                BigDecimal recv = toDecimal(m.group(4));

                out.add(new RevolutTransactionDTO(date, desc, sent, recv));
            }
        }
        return out;
    }

    private static BigDecimal toDecimal(String in) {

        String s = in.replace("−", "-")
                .replace(",", ".")
                .replace("€", "")
                .trim();

        s = s.replaceAll("[^0-9.\\-]", "");

        int lastDot = s.lastIndexOf('.');
        if (lastDot > -1) {
            String left = s.substring(0, lastDot).replace(".", "");
            String right = s.substring(lastDot);
            s = left + right;
        }

        return new BigDecimal(s);
    }

    private static LocalDate parseDate(String raw) {
        raw = raw.trim();

        if (raw.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
            return LocalDate.parse(raw, NUM_DF);
        }

        Matcher m = Pattern.compile("(\\d{1,2})\\.\\s*([a-zšđčćž]{3})\\s+(\\d{4})\\.", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                .matcher(raw);
        if (m.matches()) {
            int day = Integer.parseInt(m.group(1));
            String mon = m.group(2).toLowerCase(Locale.ROOT);
            int year = Integer.parseInt(m.group(3));

            Integer monthNum = HR_MONTH.get(mon);
            if (monthNum != null) {
                return LocalDate.of(year, monthNum, day);
            }
        }

        return null;
    }
}
