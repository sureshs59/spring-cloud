package com.expensetracker.repository;

import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA repository for Expense.
 *
 * JpaSpecificationExecutor allows dynamic filter queries built at runtime
 * (used by the filtered expenses query).
 */
@Repository
public interface ExpenseRepository
        extends JpaRepository<Expense, Long>,
                JpaSpecificationExecutor<Expense> {

    // ── Simple finders ────────────────────────────────────────────────────

    List<Expense> findByCategory(Category category);

    List<Expense> findByDateBetween(LocalDate from, LocalDate to);

    List<Expense> findByTitleContainingIgnoreCase(String keyword);

    // ── Aggregation queries ───────────────────────────────────────────────

    /**
     * Monthly totals for a given year.
     * Returns Object[] rows: [month_string, total, count]
     */
    @Query("""
        SELECT FORMATDATETIME(e.date, 'yyyy-MM') AS month,
               SUM(e.amount)  AS total,
               COUNT(e.id)    AS count
        FROM   Expense e
        WHERE  YEAR(e.date) = :year
        GROUP  BY FORMATDATETIME(e.date, 'yyyy-MM')
        ORDER  BY month ASC
    """)
    List<Object[]> findMonthlySummaries(@Param("year") int year);

    /**
     * Category totals — optionally filtered by year and month.
     * Returns Object[] rows: [category, total, count]
     */
    @Query("""
        SELECT e.category        AS category,
               SUM(e.amount)     AS total,
               COUNT(e.id)       AS count
        FROM   Expense e
        WHERE  (:year  IS NULL OR YEAR(e.date)  = :year)
          AND  (:month IS NULL OR MONTH(e.date) = :month)
        GROUP  BY e.category
        ORDER  BY total DESC
    """)
    List<Object[]> findCategorySummaries(
            @Param("year")  Integer year,
            @Param("month") Integer month);

    /**
     * Sum of amounts in a date range.
     */
    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM   Expense e
        WHERE  e.date BETWEEN :from AND :to
    """)
    Double sumByDateRange(
            @Param("from") LocalDate from,
            @Param("to")   LocalDate to);

    /**
     * Delete all expenses in a given category.
     * Returns number of rows deleted.
     */
    @Query("DELETE FROM Expense e WHERE e.category = :category")
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    int deleteByCategory(@Param("category") Category category);
}
