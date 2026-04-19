package com.expensetracker.controller;

import com.expensetracker.model.Budget;
import com.expensetracker.model.Category;
import com.expensetracker.service.BudgetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * GraphQL resolver for Budget queries, mutations, and @SchemaMapping fields.
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * KEY PATTERN: @SchemaMapping for computed/derived fields
 * ──────────────────────────────────────────────────────────────────────────────
 *
 * The Budget GraphQL type has fields "spentAmount" and "remaining" that are NOT
 * stored in the database. They are computed at runtime by joining budget limits
 * with live expense totals.
 *
 * @SchemaMapping(typeName = "Budget", field = "spentAmount")
 * public Double spentAmount(Budget budget) { ... }
 *
 * Spring GraphQL calls this method ONCE PER Budget object in the response.
 * The parent Budget object is injected automatically as the first argument.
 *
 * This is equivalent to a "field resolver" in older GraphQL Java setups.
 *
 * Add these to schema.graphqls to enable Budget support:
 * ──────────────────────────────────────────────────────────────────────────────
 *   type Budget {
 *     id:           ID!
 *     category:     Category!
 *     monthYear:    String!
 *     limitAmount:  Float!
 *     spentAmount:  Float!      # computed via @SchemaMapping
 *     remaining:    Float!      # computed via @SchemaMapping
 *     isOverBudget: Boolean!
 *   }
 *
 *   extend type Query {
 *     budgets(monthYear: String!): [Budget!]!
 *     budget(category: Category!, monthYear: String!): Budget
 *   }
 *
 *   extend type Mutation {
 *     setBudget(category: Category!, monthYear: String!, limit: Float!): Budget!
 *     deleteBudget(category: Category!, monthYear: String!): Boolean!
 *   }
 * ──────────────────────────────────────────────────────────────────────────────
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class BudgetController {

    private final BudgetService budgetService;

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * query { budgets(monthYear: "2025-03") { id category limitAmount spentAmount remaining } }
     */
    @QueryMapping
    public List<Budget> budgets(@Argument String monthYear) {
        System.out.println("GraphQL → budgets(monthYear={})"+ monthYear);
        return budgetService.getBudgets(monthYear);
    }

    /**
     * query { budget(category: FOOD, monthYear: "2025-03") { limitAmount spentAmount remaining } }
     */
    @QueryMapping
    public Budget budget(
            @Argument Category category,
            @Argument String    monthYear) {
        System.out.println("GraphQL → budget(category={}, monthYear={})"+ category+ monthYear);
        return budgetService.getBudget(category, monthYear);
    }

    // ── @SchemaMapping: computed fields on Budget ─────────────────────────────

    /**
     * Resolves Budget.spentAmount — how much has been spent in this budget's
     * category and month, pulled live from the Expense table.
     *
     * The `budget` param is the parent object Spring GraphQL injects automatically.
     */
    @SchemaMapping(typeName = "Budget", field = "spentAmount")
    public Double spentAmount(Budget budget) {
        return budgetService.computeSpentAmount(budget);
    }

    /**
     * Resolves Budget.remaining — limit minus spent.
     * Negative value means over-budget.
     */
    @SchemaMapping(typeName = "Budget", field = "remaining")
    public Double remaining(Budget budget) {
        Double spent = budgetService.computeSpentAmount(budget);
        return budgetService.computeRemaining(budget, spent);
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    /**
     * mutation { setBudget(category: FOOD, monthYear: "2025-03", limit: 300.00) { id limitAmount } }
     */
    @MutationMapping
    public Budget setBudget(
            @Argument Category category,
            @Argument String    monthYear,
            @Argument Double    limit) {
        System.out.println("GraphQL → setBudget(category={}, monthYear={}, limit={})"+
                category+ monthYear+ limit);
        return budgetService.setBudget(category, monthYear, limit);
    }

    /**
     * mutation { deleteBudget(category: FOOD, monthYear: "2025-03") }
     */
    @MutationMapping
    public Boolean deleteBudget(
            @Argument Category category,
            @Argument String    monthYear) {
        System.out.println("GraphQL → deleteBudget(category={}, monthYear={})"+ category+ monthYear);
        return budgetService.deleteBudget(category, monthYear);
    }
}
