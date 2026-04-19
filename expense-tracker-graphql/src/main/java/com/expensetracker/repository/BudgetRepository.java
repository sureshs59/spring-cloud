package com.expensetracker.repository;

import com.expensetracker.model.Budget;
import com.expensetracker.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Budget entities.
 *
 * Each budget is unique per (category + monthYear) — enforced at the DB level
 * via a unique constraint in the @Table annotation on Budget.java.
 */
@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    /** All budgets for a given month (e.g. "2025-03") */
    List<Budget> findByMonthYear(String monthYear);

    /** Single budget for a category + month combination */
    Optional<Budget> findByCategoryAndMonthYear(Category category, String monthYear);

    /** Check if any budget exists for a month */
    boolean existsByMonthYear(String monthYear);
}
