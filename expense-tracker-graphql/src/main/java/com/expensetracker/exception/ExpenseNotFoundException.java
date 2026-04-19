package com.expensetracker.exception;

/**
 * Thrown when an expense with the given ID is not found.
 * The GlobalExceptionHandler converts this into a GraphQL error response.
 */
public class ExpenseNotFoundException extends RuntimeException {

    private final Long id;

    public ExpenseNotFoundException(Long id) {
        super("Expense not found with id: " + id);
        this.id = id;
    }

    public Long getId() { return id; }
}
