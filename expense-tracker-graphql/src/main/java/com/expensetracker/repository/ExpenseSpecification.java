package com.expensetracker.repository;

import com.expensetracker.model.Category;
import com.expensetracker.model.Dtos.ExpenseFilterInput;
import com.expensetracker.model.Expense;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds a JPA Specification dynamically from the GraphQL ExpenseFilterInput.
 *
 * Each non-null filter field adds a predicate to the WHERE clause.
 * This avoids writing separate repository methods for every filter combination.
 *
 * Usage:
 *   Specification<Expense> spec = ExpenseSpecification.from(filter);
 *   repository.findAll(spec, pageable);
 */
public class ExpenseSpecification {

    private ExpenseSpecification() {}

    public static Specification<Expense> from(ExpenseFilterInput filter) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return cb.conjunction();  // no filter → return all
            }

            // Filter by category
            if (filter.category() != null) {
                predicates.add(cb.equal(root.get("category"), filter.category()));
            }

            // Filter by date range
            if (filter.fromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("date"),
                        LocalDate.parse(filter.fromDate())));
            }
            if (filter.toDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("date"),
                        LocalDate.parse(filter.toDate())));
            }

            // Filter by amount range
            if (filter.minAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), filter.minAmount()));
            }
            if (filter.maxAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), filter.maxAmount()));
            }

            // Full-text search on title or description
            if (filter.searchText() != null && !filter.searchText().isBlank()) {
                String pattern = "%" + filter.searchText().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")),       pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
