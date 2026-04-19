package com.expensetracker.controller;

import com.expensetracker.model.Category;
import com.expensetracker.model.Dtos.*;
import com.expensetracker.model.Expense;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

/**
 * GraphQL Controller — maps every schema field to a Java method.
 *
 * Annotation guide:
 *  @QueryMapping    — maps a method to a Query field (method name = field name)
 *  @MutationMapping — maps a method to a Mutation field
 *  @SubscriptionMapping — maps a method to a Subscription field (returns Flux<T>)
 *  @Argument        — injects a named argument from the GraphQL operation
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ExpenseController {

	@Autowired
    private final ExpenseService      expenseService;
    private final SubscriptionService subscriptionService;

    // ════════════════════════════════════════════════════════════════════════
    //  QUERIES
    // ════════════════════════════════════════════════════════════════════════

    /**
     * query { expense(id: "1") { id title amount } }
     */
    @QueryMapping
    public Expense expense(@Argument Long id) {
       // log.debug("GraphQL → expense(id={})", id);
        return expenseService.findById(id);
    }

    /**
     * query { expenses(filter: {...}, pagination: {...}) { content { ... } totalElements } }
     */
    @QueryMapping
    public ExpensePage expenses(
            @Argument ExpenseFilterInput filter,
            @Argument PaginationInput    pagination) {
       // log.debug("GraphQL → expenses(filter={}, pagination={})", filter, pagination);
        return expenseService.findExpenses(filter, pagination);
    }

    /**
     * query { expensesByCategory(category: FOOD) { id title amount } }
     */
    @QueryMapping
    public java.util.List<Expense> expensesByCategory(@Argument Category category) {
        return expenseService.findByCategory(category);
    }

    /**
     * query { monthlySummaries(year: 2025) { month total count avgPerDay } }
     */
    @QueryMapping
    public java.util.List<MonthlySummary> monthlySummaries(@Argument Integer year) {
        return expenseService.getMonthlySummaries(year);
    }

    /**
     * query { categorySummaries(year: 2025, month: 1) { category total percentage } }
     */
    @QueryMapping
    public java.util.List<CategorySummary> categorySummaries(
            @Argument Integer year,
            @Argument Integer month) {
        return expenseService.getCategorySummaries(year, month);
    }

    /**
     * query { totalSpent(fromDate: "2025-01-01", toDate: "2025-01-31") }
     */
    @QueryMapping
    public Double totalSpent(
            @Argument String fromDate,
            @Argument String toDate) {
        return expenseService.getTotalSpent(fromDate, toDate);
    }

    /**
     * query { predictNextMonth }
     */
    @QueryMapping
    public Double predictNextMonth() {
        return expenseService.predictNextMonth();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  MUTATIONS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * mutation { createExpense(input: { title: "Lunch", amount: 12.50, ... }) { id title } }
     */
    @MutationMapping
    public Expense createExpense(@Argument CreateExpenseInput input) {
       // log.info("GraphQL → createExpense({})", input);
        Expense created = expenseService.createExpense(input);
        subscriptionService.publishCreated(created);   // notify subscribed clients
        return created;
    }

    /**
     * mutation { updateExpense(id: "5", input: { amount: 15.00 }) { id title amount } }
     */
    @MutationMapping
    public Expense updateExpense(
            @Argument Long              id,
            @Argument UpdateExpenseInput input) {
       // log.info("GraphQL → updateExpense(id={}, input={})", id, input);
        Expense updated = expenseService.updateExpense(id, input);
        subscriptionService.publishUpdated(updated);   // notify subscribed clients
        return updated;
    }

    /**
     * mutation { deleteExpense(id: "5") { success message deletedId } }
     */
    @MutationMapping
    public DeleteResult deleteExpense(@Argument Long id) {
       // log.info("GraphQL → deleteExpense(id={})", id);
        DeleteResult result = expenseService.deleteExpense(id);
        if (result.success()) {
            subscriptionService.publishDeleted(id);    // notify subscribed clients
        }
        return result;
    }

    /**
     * mutation { deleteByCategory(category: FOOD) }  → returns count of deleted rows
     */
    @MutationMapping
    public Integer deleteByCategory(@Argument Category category) {
        //log.info("GraphQL → deleteByCategory(category={})", category);
        return expenseService.deleteByCategory(category);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SUBSCRIPTIONS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * subscription { expenseCreated { id title amount category } }
     *
     * Client connects over WebSocket. Every time createExpense mutation runs,
     * this Flux emits the new Expense and the client receives a push.
     */
    @SubscriptionMapping
    public Flux<Expense> expenseCreated() {
      //  log.debug("New subscriber: expenseCreated");
        return subscriptionService.expenseCreatedFlux();
    }

    /**
     * subscription { expenseUpdated { id title amount } }
     */
    @SubscriptionMapping
    public Flux<Expense> expenseUpdated() {
      //  log.debug("New subscriber: expenseUpdated");
        return subscriptionService.expenseUpdatedFlux();
    }

    /**
     * subscription { expenseDeleted }   → returns the deleted expense's ID
     */
    @SubscriptionMapping
    public Flux<Long> expenseDeleted() {
      //  log.debug("New subscriber: expenseDeleted");
        return subscriptionService.expenseDeletedFlux();
    }
}
