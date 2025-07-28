// src/main/java/service/RevolutImportService.java
package service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import model.entity.Transaction;
import model.response.ServiceResponseDTO;
import model.response.ServiceResponseDirector;
import model.external.RevolutTransactionDTO;
import repository.TransactionRepository;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@ApplicationScoped
public class RevolutImportService {

    @Inject
    RevolutStatementService parser;
    @Inject
    CategoryService categoryService;
    @Inject
    TransactionRepository txRepo;

    @Transactional
    public ServiceResponseDTO<List<Transaction>> importMonthlyPdf(String userSub, java.io.InputStream pdf) {
        try {
            List<RevolutTransactionDTO> lines = parser.parseMonthlyStatement(pdf);
            var cat = categoryService.getOrCreateRevolutCategory();
            Long catId = cat.getId();

            List<Transaction> saved = lines.stream().map(dto -> {
                var t = new Transaction();
                t.setUserSub(userSub);

                if (dto.sentAmount().signum() > 0) {
                    t.setType("E");
                    t.setAmount(dto.sentAmount());
                } else {
                    t.setType("I");
                    t.setAmount(dto.receivedAmount());
                }

                t.setCategoryId(catId);
                OffsetDateTime when = dto.date()
                        .atTime(LocalTime.MIDNIGHT)
                        .atOffset(ZoneOffset.UTC);
                t.setTxTime(when);
                t.persist();
                return t;
            }).toList();

            return ServiceResponseDirector.successCreated(
                    saved,
                    "Imported " + saved.size() + " revolut transactions"
            );
        } catch (Exception e) {
            return ServiceResponseDirector.errorBadRequest("Failed to parse PDF");
        }
    }
}
