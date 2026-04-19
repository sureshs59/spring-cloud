package com.expensetracker.service;

import com.expensetracker.model.Expense;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Manages reactive event streams for GraphQL Subscriptions.
 *
 * Three independent sinks:
 *  - expenseCreatedSink  → pushed when a new expense is saved
 *  - expenseUpdatedSink  → pushed when an expense is modified
 *  - expenseDeletedSink  → pushed (with the deleted ID) when an expense is removed
 *
 * The GraphQL controller exposes these as Flux<T> via @SubscriptionMapping.
 * Spring Boot + graphql-java converts each emitted item into a WebSocket push.
 *
 * Sinks.many().multicast() — every subscriber (connected client) receives every event.
 * onBackpressureBuffer(16) — buffers up to 16 events if a slow client can't keep up.
 */
@Service
@Slf4j
public class SubscriptionService {

    // ── Sinks (hot publishers) ────────────────────────────────────────────────

    private final Sinks.Many<Expense> expenseCreatedSink =
            Sinks.many().multicast().onBackpressureBuffer(16);

    private final Sinks.Many<Expense> expenseUpdatedSink =
            Sinks.many().multicast().onBackpressureBuffer(16);

    private final Sinks.Many<Long> expenseDeletedSink =
            Sinks.many().multicast().onBackpressureBuffer(16);

    // ── Public Flux streams (consumed by @SubscriptionMapping) ────────────────

    public Flux<Expense> expenseCreatedFlux() {
        return expenseCreatedSink.asFlux();
    }

    public Flux<Expense> expenseUpdatedFlux() {
        return expenseUpdatedSink.asFlux();
    }

    public Flux<Long> expenseDeletedFlux() {
        return expenseDeletedSink.asFlux();
    }

    // ── Emit methods (called from ExpenseController after each mutation) ───────

    public void publishCreated(Expense expense) {
        System.out.println("Publishing expenseCreated event: id={}"+expense.getId());
        Sinks.EmitResult result = expenseCreatedSink.tryEmitNext(expense);
        if (result.isFailure()) {
        	System.out.println("Failed to emit expenseCreated: {}"+ result);
        }
    }

    public void publishUpdated(Expense expense) {
        System.out.println("Publishing expenseUpdated event: id={}"+ expense.getId());
        expenseUpdatedSink.tryEmitNext(expense);
    }

    public void publishDeleted(Long id) {
        System.out.println("Publishing expenseDeleted event: id={}"+ id);
        expenseDeletedSink.tryEmitNext(id);
    }
}
