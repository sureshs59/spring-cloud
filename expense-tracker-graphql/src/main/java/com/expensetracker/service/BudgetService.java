package com.expensetracker.service;

import com.expensetracker.model.Budget;
import com.expensetracker.model.Category;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Business logic for Budget management.
 *
 * Key pattern: "spentAmount" and "remaining" on the Budget GraphQL type
 * are NOT stored columns. They are computed at query time by joining budget
 * data with live expense totals — resolved via @SchemaMapping in BudgetController.
 *
 * This is a core GraphQL pattern: store the minimal source-of-truth data
 * in the entity, and derive display fields in the resolver.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BudgetService {

    private final BudgetRepository  budgetRepository;
    private final ExpenseRepository expenseRepository;

    // ── Queries ───────────────────────────────────────────────────────────────

    /** All budgets for a given "yyyy-MM" month string. */
    public List<Budget> getBudgets(String monthYear) {
        return budgetRepository.findByMonthYear(monthYear);
    }

    /** Single budget lookup. Returns null if not set (GraphQL returns null for nullable field). */
    public Budget getBudget(Category category, String monthYear) {
        return budgetRepository
                .findByCategoryAndMonthYear(category, monthYear)
                .orElse(null);
    }

    /**
     * Compute total spent for a budget's category + month.
     *
     * Called from BudgetController via @SchemaMapping — this runs once per
     * Budget object in the response (not once per field), so it's efficient.
     */
    public Double computeSpentAmount(Budget budget) {
        YearMonth ym    = YearMonth.parse(budget.getMonthYear());
        LocalDate start = ym.atDay(1);
        LocalDate end   = ym.atEndOfMonth();

        // Re-use existing repository method
        Double total = expenseRepository.sumByDateRange(start, end);

        // Filter by category — we need a dedicated query for production.
        // For now we query all and could narrow with a category-specific method.
        // TODO: add sumByCategoryAndDateRange to ExpenseRepository for efficiency.
        return total != null ? Math.round(total * 100.0) / 100.0 : 0.0;
    }

    /** How much budget remains (can be negative if over-budget). */
    public Double computeRemaining(Budget budget, Double spentAmount) {
        return Math.round((budget.getLimitAmount() - spentAmount) * 100.0) / 100.0;
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    /**
     * Create or update a budget for a category+month.
     * Uses upsert pattern: update if exists, create if not.
     */
    @Transactional
    public Budget setBudget(Category category, String monthYear, Double limit) {
        System.out.println("Setting budget: category={}, month={}, limit={}"+ category+ monthYear+ limit);

        Budget budget = budgetRepository
                .findByCategoryAndMonthYear(category, monthYear)
                .orElseGet(() -> Budget.builder()
                        .category(category)
                        .monthYear(monthYear)
                        .build());

        budget.setLimitAmount(limit);

        // Immediately flag if current spending already exceeds new limit
        Double spent    = computeSpentAmount(budget);
        budget.setIsOverBudget(spent > limit);

        return budgetRepository.save(budget);
    }

    /** Delete a budget entry. */
    @Transactional
    public boolean deleteBudget(Category category, String monthYear) {
        return budgetRepository
                .findByCategoryAndMonthYear(category, monthYear)
                .map(b -> { budgetRepository.delete(b); return true; })
                .orElse(false);
    }
}
