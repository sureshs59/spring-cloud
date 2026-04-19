package com.expensetracker.model;

/**
 * All GraphQL input types and response DTOs are defined here as Java records.
 * Spring Boot's GraphQL auto-maps @Argument values to these types by field name.
 */
public class Dtos {

    // ── Input Records (mirror the GraphQL input types) ─────────────────────

    /**
     * Maps to:  input CreateExpenseInput { ... }
     */
    public record CreateExpenseInput(
            String   title,
            Double   amount,
            Category category,
            String   date,          // ISO-8601 string e.g. "2025-01-15"
            String   description
    ) {}

    /**
     * Maps to:  input UpdateExpenseInput { ... }
     * All fields are nullable — only provided fields are updated.
     */
    public record UpdateExpenseInput(
            String   title,
            Double   amount,
            Category category,
            String   date,
            String   description
    ) {}

    /**
     * Maps to:  input ExpenseFilterInput { ... }
     */
    public record ExpenseFilterInput(
            Category category,
            String   fromDate,
            String   toDate,
            Double   minAmount,
            Double   maxAmount,
            String   searchText
    ) {}

    /**
     * Maps to:  input PaginationInput { ... }
     */
    public record PaginationInput(
            Integer page,
            Integer size,
            String  sortBy,
            String  direction
    ) {}

    // ── Response Records (mirror the GraphQL output types) ─────────────────

    /**
     * Maps to:  type ExpensePage { ... }
     */
    public record ExpensePage(
            java.util.List<Expense> content,
            long    totalElements,
            int     totalPages,
            int     currentPage,
            int     pageSize
    ) {}

    /**
     * Maps to:  type MonthlySummary { ... }
     */
    public record MonthlySummary(
            String month,       // "2025-01"
            Double total,
            Long   count,
            Double avgPerDay
    ) {}

    /**
     * Maps to:  type CategorySummary { ... }
     */
    public record CategorySummary(
            Category category,
            Double   total,
            Long     count,
            Double   percentage
    ) {}

    /**
     * Maps to:  type DeleteResult { ... }
     */
    public record DeleteResult(
            boolean success,
            String  message,
            Long    deletedId
    ) {}
}
