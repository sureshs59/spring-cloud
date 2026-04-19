package com.expensetracker.service;

import com.expensetracker.exception.ExpenseNotFoundException;
import com.expensetracker.model.Category;
import com.expensetracker.model.Dtos.*;
import com.expensetracker.model.Expense;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.repository.ExpenseSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Core business logic for expense operations.
 * All GraphQL resolvers delegate to this service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * Fetch a single expense by ID.
     * Throws ExpenseNotFoundException if not found (GraphQL returns an error).
     */
    public Expense findById(Long id) {
        System.out.println("Fetching expense id={}"+ id);
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ExpenseNotFoundException(id));
    }

    /**
     * Fetch expenses with optional filtering and pagination.
     */
    public ExpensePage findExpenses(ExpenseFilterInput filter, PaginationInput pagination) {

        // Build pageable from pagination input (with safe defaults)
        int page      = pagination != null && pagination.page()      != null ? pagination.page()      : 0;
        int size      = pagination != null && pagination.size()      != null ? pagination.size()      : 10;
        String sortBy = pagination != null && pagination.sortBy()    != null ? pagination.sortBy()    : "DATE";
        String dir    = pagination != null && pagination.direction() != null ? pagination.direction() : "DESC";

        String sortField = switch (sortBy) {
            case "AMOUNT"   -> "amount";
            case "CATEGORY" -> "category";
            default         -> "date";
        };

        Sort sort = dir.equalsIgnoreCase("ASC")
                ? Sort.by(sortField).ascending()
                : Sort.by(sortField).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        // Build specification from filter
        Specification<Expense> spec = ExpenseSpecification.from(filter);
        Page<Expense> result = expenseRepository.findAll(spec, pageable);

        return new ExpensePage(
                result.getContent(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
        );
    }

    /**
     * All expenses for a given category.
     */
    public List<Expense> findByCategory(Category category) {
        return expenseRepository.findByCategory(category);
    }

    /**
     * Monthly summaries for a given year — used for the bar chart.
     */
    public List<MonthlySummary> getMonthlySummaries(int year) {
        List<Object[]> rows = expenseRepository.findMonthlySummaries(year);
        List<MonthlySummary> summaries = new ArrayList<>();

        for (Object[] row : rows) {
            String month    = (String) row[0];
            Double total    = (Double) row[1];
            Long   count    = (Long)   row[2];
            int    daysInMonth = YearMonth.parse(month).lengthOfMonth();
            Double avgPerDay   = count > 0 ? Math.round((total / daysInMonth) * 100.0) / 100.0 : 0.0;
            summaries.add(new MonthlySummary(month, total, count, avgPerDay));
        }
        return summaries;
    }

    /**
     * Category breakdown — used for the pie chart.
     */
    public List<CategorySummary> getCategorySummaries(Integer year, Integer month) {
        List<Object[]> rows = expenseRepository.findCategorySummaries(year, month);

        // Calculate grand total for percentage
        double grandTotal = rows.stream()
                .mapToDouble(r -> (Double) r[1])
                .sum();

        return rows.stream().map(row -> {
            Category cat   = (Category) row[0];
            Double   total = (Double)   row[1];
            Long     count = (Long)     row[2];
            Double   pct   = grandTotal > 0
                    ? Math.round((total / grandTotal * 100) * 10.0) / 10.0
                    : 0.0;
            return new CategorySummary(cat, total, count, pct);
        }).toList();
    }

    /**
     * Total amount spent between two dates.
     */
    public Double getTotalSpent(String fromDate, String toDate) {
        return expenseRepository.sumByDateRange(
                LocalDate.parse(fromDate),
                LocalDate.parse(toDate));
    }

    /**
     * Predict next month's spending using a simple moving average
     * of the last 3 months' totals.
     *
     * Replace this with an ML model call (FastAPI) for a real predictor.
     */
    public Double predictNextMonth() {
        LocalDate today   = LocalDate.now();
        LocalDate threeMonthsAgo = today.minusMonths(3).withDayOfMonth(1);

        Double total = expenseRepository.sumByDateRange(threeMonthsAgo, today);
        long days = java.time.temporal.ChronoUnit.DAYS.between(threeMonthsAgo, today);

        if (days == 0) return 0.0;

        // Daily average × days in next month
        double dailyAvg    = total / days;
        int    nextMonthLen = YearMonth.now().plusMonths(1).lengthOfMonth();
        double prediction   = dailyAvg * nextMonthLen;

        // Round to 2 decimal places
        return Math.round(prediction * 100.0) / 100.0;
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    /**
     * Create a new expense from the GraphQL input.
     */
    @Transactional
    public Expense createExpense(CreateExpenseInput input) {
        System.out.println("Creating expense: title={}, amount={}"+ input.title()+ input.amount());

        Expense expense = Expense.builder()
                .title(input.title())
                .amount(input.amount())
                .category(input.category())
                .date(LocalDate.parse(input.date()))
                .description(input.description())
                .build();

        return expenseRepository.save(expense);
    }

    /**
     * Partially update an existing expense — only non-null fields are changed.
     */
    @Transactional
    public Expense updateExpense(Long id, UpdateExpenseInput input) {
        System.out.println("Updating expense id={}"+ id);

        Expense expense = findById(id);

        if (input.title()       != null) expense.setTitle(input.title());
        if (input.amount()      != null) expense.setAmount(input.amount());
        if (input.category()    != null) expense.setCategory(input.category());
        if (input.date()        != null) expense.setDate(LocalDate.parse(input.date()));
        if (input.description() != null) expense.setDescription(input.description());

        return expenseRepository.save(expense);
    }

    /**
     * Delete an expense by ID and return a structured result.
     */
    @Transactional
    public DeleteResult deleteExpense(Long id) {
        System.out.println("Deleting expense id={}"+ id);

        Expense expense = findById(id);  // throws if not found
        expenseRepository.deleteById(id);

        return new DeleteResult(true, "Expense '" + expense.getTitle() + "' deleted.", id);
    }

    /**
     * Delete all expenses in a category and return count of deleted rows.
     */
    @Transactional
    public int deleteByCategory(Category category) {
        System.out.println("Deleting all expenses in category={}"+ category);
        return expenseRepository.deleteByCategory(category);
    }
}
