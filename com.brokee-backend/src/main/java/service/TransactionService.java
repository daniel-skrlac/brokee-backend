package service;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import mapper.TransactionMapper;
import model.entity.Transaction;
import model.helper.PagedResponseDTO;
import model.home.FullTxRequestDTO;
import model.home.QuickTxRequestDTO;
import model.home.TxResponseDTO;
import model.response.ServiceResponse;
import model.response.ServiceResponseDirector;
import model.tracking.CategoryBreakdownDTO;
import model.tracking.LocationDTO;
import model.tracking.SpendingVsIncomeDTO;
import repository.BudgetRepository;
import repository.CategoryRepository;
import repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@ApplicationScoped
public class TransactionService {

    @Inject
    LocationService locationService;

    @Inject
    TransactionRepository txRepo;

    @Inject
    BudgetRepository budgetRepository;

    @Inject
    CategoryRepository categoryRepository;

    @Inject
    TransactionMapper txMap;

    @Inject
    NotificationService notifier;

    public ServiceResponse<TxResponseDTO> findById(String userSub, Long id) {
        Transaction t = txRepo.findByIdAndUser(userSub, id);
        if (t == null) {
            return ServiceResponseDirector.errorNotFound("Transaction not found");
        }
        return ServiceResponseDirector.successOk(
                txMap.entityToResponse(t),
                "OK"
        );
    }

    public ServiceResponse<List<SpendingVsIncomeDTO>> spendingVsIncome(String userSub, int year) {
        List<SpendingVsIncomeDTO> list = txRepo
                .findSpendingVsIncomeByYear(userSub, year)
                .stream()
                .map(t -> new SpendingVsIncomeDTO(
                        t.get("month", String.class),
                        t.get("expenses", BigDecimal.class),
                        t.get("income", BigDecimal.class)
                ))
                .collect(Collectors.toList());

        return ServiceResponseDirector.successOk(list, "OK");
    }

    public ServiceResponse<List<CategoryBreakdownDTO>> categoryBreakdown(String userSub, String monthKey) {
        List<CategoryBreakdownDTO> list = txRepo
                .findCategoryBreakdown(userSub, monthKey)
                .stream()
                .map(t -> new CategoryBreakdownDTO(
                        t.get("category", String.class),
                        t.get("amount", BigDecimal.class)
                ))
                .collect(Collectors.toList());

        return ServiceResponseDirector.successOk(list, "OK");
    }

    public ServiceResponse<List<LocationDTO>> topLocations(String userSub, int limit) {
        List<LocationDTO> list = txRepo
                .findTopLocations(userSub, limit)
                .stream()
                .map(t -> new LocationDTO(
                        t.get("latitude", BigDecimal.class),
                        t.get("longitude", BigDecimal.class),
                        t.get("label", String.class),
                        t.get("amount", BigDecimal.class)
                ))
                .collect(Collectors.toList());

        return ServiceResponseDirector.successOk(list, "OK");
    }

    public ServiceResponse<List<TxResponseDTO>> recent(String userSub, int limit) {
        List<TxResponseDTO> dtos = txRepo
                .findRecent(userSub, limit)
                .stream()
                .map(txMap::entityToResponse)
                .toList();

        return ServiceResponseDirector.successOk(dtos, "OK");
    }

    public ServiceResponse<PagedResponseDTO<TxResponseDTO>> page(
            String userSub, int page, int size) {

        PanacheQuery<Transaction> query = txRepo.findByUserSorted(userSub);

        long total = query.count();
        List<TxResponseDTO> dtos = query
                .page(Page.of(page, size))
                .list()
                .stream()
                .map(txMap::entityToResponse)
                .toList();

        PagedResponseDTO<TxResponseDTO> paged = new PagedResponseDTO<>(
                dtos,
                page,
                size,
                total
        );

        return ServiceResponseDirector.successOk(paged, "OK");
    }

    public ServiceResponse<BigDecimal> getBalance(String userSub) {
        BigDecimal income = txRepo.sumByType(userSub, "I");
        BigDecimal expense = txRepo.sumByType(userSub, "E");
        return ServiceResponseDirector.successOk(
                income.subtract(expense),
                "OK"
        );
    }

    public ServiceResponse<List<TxResponseDTO>> findByDateRange(
            String userSub,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        var dtos = txRepo.findByUserAndDateRange(userSub, from, to)
                .stream().map(txMap::entityToResponse).toList();
        return ServiceResponseDirector.successOk(dtos, "OK");
    }

    public ServiceResponse<Map<String, BigDecimal>> findDailyExpenses(
            String userSub, int days
    ) {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        var start = now.minusDays(days - 1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        var sums = txRepo.findByUserAndDateRange(userSub, start, now).stream()
                .filter(t -> "E".equals(t.getType()))
                .collect(Collectors.groupingBy(
                        t -> t.getTxTime().toLocalDate().toString(),
                        TreeMap::new,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Transaction::getAmount,
                                BigDecimal::add
                        )
                ));

        return ServiceResponseDirector.successOk(sums, "OK");
    }

    public ServiceResponse<Map<String, BigDecimal>> findMonthlyExpenses(
            String userSub, int year
    ) {
        LocalDate yStart = LocalDate.of(year, 1, 1);
        LocalDate yEnd = LocalDate.of(year, 12, 31);
        var from = yStart.atStartOfDay().atOffset(ZoneOffset.UTC);
        var to = yEnd.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);

        var sums = txRepo.findByUserAndDateRange(userSub, from, to).stream()
                .filter(t -> "E".equals(t.getType()))
                .collect(Collectors.groupingBy(
                        t -> String.format("%d-%02d",
                                t.getTxTime().getYear(),
                                t.getTxTime().getMonthValue()),
                        TreeMap::new,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Transaction::getAmount,
                                BigDecimal::add
                        )
                ));

        return ServiceResponseDirector.successOk(sums, "OK");
    }

    public ServiceResponse<TxResponseDTO> quickAdd(String userSub, QuickTxRequestDTO dto) {
        Transaction t = txMap.quickRequestToEntity(dto);
        t.setUserSub(userSub);
        t.persist();
        return ServiceResponseDirector.successCreated(
                txMap.entityToResponse(t), "Successfully Created");
    }

    @Transactional
    public ServiceResponse<TxResponseDTO> create(String userSub, FullTxRequestDTO dto) {
        Transaction t = txMap.fullRequestToEntity(dto);
        t.setUserSub(userSub);
        t.persist();

        t.setLocationName(locationService.getLocationName(
                t.getLatitude().doubleValue(),
                t.getLongitude().doubleValue()
        ));
        t.persist();

        if (t.getType().equals("E") &&
                t.getAmount().compareTo(new BigDecimal("1000.00")) > 0 &&
                (t.getNote() == null || t.getNote().isBlank())) {

            String catName = categoryRepository.findCategoryNameById(t.getCategoryId());
            notifier.sendToUser(
                    userSub,
                    "💸 Large Transaction",
                    "Large transaction detected: " + t.getAmount() + " on " + catName + "."
            );
        }

        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        BigDecimal spent = txRepo.sumExpensesForCategorySince(userSub, t.getCategoryId(), startOfMonth);
        BigDecimal budget = budgetRepository.findAmountByUserAndCategory(userSub, t.getCategoryId());
        if (budget != null && spent.compareTo(budget) > 0) {
            String catName = categoryRepository.findCategoryNameById(t.getCategoryId());
            notifier.sendToUser(
                    userSub,
                    "🚨 Budget Exceeded",
                    "You've exceeded your " + catName + " budget!"
            );
        }

        return ServiceResponseDirector.successCreated(
                txMap.entityToResponse(t),
                "Successfully Created"
        );
    }

    @Transactional
    public ServiceResponse<TxResponseDTO> update(
            String userSub,
            Long id,
            FullTxRequestDTO dto
    ) {
        Transaction t = txRepo.findByIdAndUser(userSub, id);
        if (t == null) {
            return ServiceResponseDirector.errorNotFound("Transaction not found");
        }
        txMap.updateFromFullDto(dto, t);
        t.setLocationName(locationService.getLocationName(
                t.getLatitude().doubleValue(),
                t.getLongitude().doubleValue()
        ));
        return ServiceResponseDirector.successOk(
                txMap.entityToResponse(t),
                "Successfully Updated"
        );
    }

    public ServiceResponse<Boolean> delete(String userSub, Long id) {
        boolean deleted = txRepo.deleteByIdAndUser(userSub, id);
        if (!deleted) {
            return ServiceResponseDirector.errorNotFound("Transaction not found");
        }
        return ServiceResponseDirector.successOk(true, "Successfully Deleted");
    }
}
