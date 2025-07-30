package service;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import mapper.TransactionMapper;
import model.entity.Category;
import model.entity.Transaction;
import model.helper.PagedResponseDTO;
import model.home.FullTxRequestDTO;
import model.home.QuickTxRequestDTO;
import model.home.TxResponseDTO;
import model.response.ServiceResponseDTO;
import model.response.ServiceResponseDirector;
import model.tracking.CategoryBreakdownDTO;
import model.tracking.LocationDTO;
import model.tracking.SpendingVsIncomeDTO;
import repository.BudgetRepository;
import repository.CategoryRepository;
import repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    public ServiceResponseDTO<TxResponseDTO> findById(String userSub, Long id) {
        Transaction t = txRepo.findByIdAndUser(userSub, id);
        if (t == null) {
            return ServiceResponseDirector.errorNotFound("Transaction not found");
        }
        return ServiceResponseDirector.successOk(
                txMap.entityToResponse(t),
                "OK"
        );
    }

    public ServiceResponseDTO<List<SpendingVsIncomeDTO>> spendingVsIncome(String userSub, int year) {
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

    public ServiceResponseDTO<List<CategoryBreakdownDTO>> categoryBreakdown(String userSub, String monthKey) {
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

    public ServiceResponseDTO<List<LocationDTO>> topLocations(String userSub, int limit) {
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

    public ServiceResponseDTO<List<TxResponseDTO>> recent(String userSub, int limit) {
        List<TxResponseDTO> dtos = txRepo
                .findRecent(userSub, limit)
                .stream()
                .map(txMap::entityToResponse)
                .toList();

        return ServiceResponseDirector.successOk(dtos, "OK");
    }

    public ServiceResponseDTO<PagedResponseDTO<TxResponseDTO>> page(
            String userSub,
            int page,
            int size,
            String type,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String dueFromStr,
            String dueToStr,
            String note,
            String categoryName
    ) {
        LocalDateTime dueFrom = null, dueTo = null;
        try {
            if (dueFromStr != null) dueFrom = OffsetDateTime.parse(dueFromStr).toLocalDateTime();
            if (dueToStr != null) dueTo = OffsetDateTime.parse(dueToStr).toLocalDateTime();
        } catch (DateTimeParseException ex) {
            return ServiceResponseDirector.errorBadRequest("Invalid date format");
        }

        PanacheQuery<Transaction> query = txRepo.findByUserWithFilters(
                userSub, type, minAmount, maxAmount, dueFrom, dueTo,
                note, categoryName
        );

        long total = query.count();
        List<TxResponseDTO> dtos = query
                .page(Page.of(page, size))
                .list()
                .stream()
                .map(txMap::entityToResponse)
                .toList();

        var paged = new PagedResponseDTO<>(dtos, page, size, total);
        return ServiceResponseDirector.successOk(paged, "OK");
    }

    public ServiceResponseDTO<BigDecimal> getBalance(String userSub) {
        BigDecimal income = txRepo.sumByType(userSub, "I");
        BigDecimal expense = txRepo.sumByType(userSub, "E");
        return ServiceResponseDirector.successOk(
                income.subtract(expense),
                "OK"
        );
    }

    public ServiceResponseDTO<List<TxResponseDTO>> findByDateRange(
            String userSub,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        var dtos = txRepo.findByUserAndDateRange(userSub, from, to)
                .stream().map(txMap::entityToResponse).toList();
        return ServiceResponseDirector.successOk(dtos, "OK");
    }

    public ServiceResponseDTO<Map<String, BigDecimal>> findDailyExpenses(
            String userSub, int days
    ) {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        var start = now.minusDays(days - 1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        var sums = txRepo.findByUserAndDateRange(userSub, start, now).stream()
                .collect(Collectors.groupingBy(
                        t -> t.getTxTime().toLocalDate().toString(),
                        TreeMap::new,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                t -> {
                                    // map expense → negative, income → positive
                                    return "E".equals(t.getType())
                                            ? t.getAmount().negate()
                                            : t.getAmount();
                                },
                                BigDecimal::add
                        )
                ));
        return ServiceResponseDirector.successOk(sums, "OK");
    }

    public ServiceResponseDTO<Map<String, BigDecimal>> findMonthlyExpenses(
            String userSub, int year
    ) {
        LocalDate yStart = LocalDate.of(year, 1, 1);
        LocalDate yEnd = LocalDate.of(year, 12, 31);
        var from = yStart.atStartOfDay().atOffset(ZoneOffset.UTC);
        var to = yEnd.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);

        var sums = txRepo.findByUserAndDateRange(userSub, from, to).stream()
                .collect(Collectors.groupingBy(
                        t -> String.format("%d-%02d",
                                t.getTxTime().getYear(),
                                t.getTxTime().getMonthValue()),
                        TreeMap::new,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                t -> {
                                    return "E".equals(t.getType())
                                            ? t.getAmount().negate()
                                            : t.getAmount();
                                },
                                BigDecimal::add
                        )
                ));

        return ServiceResponseDirector.successOk(sums, "OK");
    }

    @Transactional
    public ServiceResponseDTO<TxResponseDTO> quickAdd(String userSub, QuickTxRequestDTO dto) {
        Transaction t = txMap.quickRequestToEntity(dto);
        if (dto.categoryId() == null) {
            Optional<Category> category = categoryRepository.findByName("General");
            category.ifPresent(value -> t.setCategoryId(value.getId()));
        }
        t.setUserSub(userSub);
        t.persist();
        return ServiceResponseDirector.successCreated(
                txMap.entityToResponse(t), "Successfully Created");
    }

    @Transactional
    public ServiceResponseDTO<TxResponseDTO> create(String userSub, FullTxRequestDTO dto) {
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
    public ServiceResponseDTO<TxResponseDTO> update(
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

    public ServiceResponseDTO<Boolean> delete(String userSub, Long id) {
        boolean deleted = txRepo.deleteByIdAndUser(userSub, id);
        if (!deleted) {
            return ServiceResponseDirector.errorNotFound("Transaction not found");
        }
        return ServiceResponseDirector.successOk(true, "Successfully Deleted");
    }
}
