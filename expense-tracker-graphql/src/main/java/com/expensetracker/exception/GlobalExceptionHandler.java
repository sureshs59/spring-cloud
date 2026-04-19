package com.expensetracker.exception;

import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.stereotype.Component;
import org.springframework.graphql.execution.*;

/**
 * Global exception handler for GraphQL resolver errors.
 *
 * Spring GraphQL calls resolveToSingleError() when any @QueryMapping,
 * @MutationMapping, or @SubscriptionMapping throws an exception.
 *
 * Instead of returning a 500 HTTP error, we return a structured GraphQL
 * error in the "errors" array of the response JSON — exactly what GraphQL
 * clients expect.
 *
 * Response shape on error:
 * {
 *   "data":   null,
 *   "errors": [{ "message": "...", "extensions": { "code": "NOT_FOUND" } }]
 * }
 */
@Component
@Slf4j
public class GlobalExceptionHandler extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {

        // 404 — expense not found
        if (ex instanceof ExpenseNotFoundException notFound) {
            //log.warn("Expense not found: id={}", notFound.getId());
            return GraphqlErrorBuilder.newError(env)
                    .message(notFound.getMessage())
                    .errorType(org.springframework.graphql.execution.ErrorType.NOT_FOUND)
                    //.extension("code",      "NOT_FOUND")
                    //.extension("expenseId", notFound.getId())
                    .build();
        }

        // 400 — validation failure (@NotBlank, @Positive, etc.)
        if (ex instanceof ConstraintViolationException cve) {
            String details = cve.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(cve.getMessage());

            //log.warn("Validation error: {}", details);
            return GraphqlErrorBuilder.newError(env)
                    .message("Validation failed: " + details)
                    .errorType(org.springframework.graphql.execution.ErrorType.BAD_REQUEST)
                    
                    .build();
        }

        // 400 — illegal argument (e.g. invalid date format)
        if (ex instanceof IllegalArgumentException iae) {
            //log.warn("Bad argument: {}", iae.getMessage());
            return GraphqlErrorBuilder.newError(env)
                    .message("Invalid input: " + iae.getMessage())
                    .errorType(org.springframework.graphql.execution.ErrorType.BAD_REQUEST)
                    //.extension("code", "INVALID_INPUT")
                    .build();
        }

        // 500 — unexpected error (do not leak internal details to clients)
        //log.error("Unhandled GraphQL error in field '{}': {}",
          //      env.getField().getName(), ex.getMessage(), ex);
        return GraphqlErrorBuilder.newError(env)
                .message("An internal error occurred. Please try again.")
                .errorType(org.springframework.graphql.execution.ErrorType.INTERNAL_ERROR)
                //.extension("code", "INTERNAL_ERROR")
                .build();
    }
}
