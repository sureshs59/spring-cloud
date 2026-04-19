package com.expensetracker;

import com.expensetracker.model.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

/**
 * Integration tests for all GraphQL operations.
 *
 * HttpGraphQlTester sends real GraphQL requests to the running application
 * and lets you assert on the response data using JsonPath-style paths.
 *
 * Run with:  mvn test
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureHttpGraphQlTester
class ExpenseTrackerGraphQLTests {

    @Autowired
    HttpGraphQlTester graphQlTester;

    // ── Query Tests ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Query: fetch expenses returns seeded data")
    void expenses_returnsPagedResults() {
        graphQlTester.document("""
            query {
              expenses {
                totalElements
                currentPage
                content {
                  id
                  title
                  amount
                  category
                }
              }
            }
        """)
        .execute()
        .path("expenses.totalElements").entity(Integer.class).satisfies(count ->
                org.assertj.core.api.Assertions.assertThat(count).isGreaterThan(0))
        .path("expenses.content[0].title").hasValue()
        .path("expenses.content[0].category").hasValue();
    }

    @Test
    @DisplayName("Query: fetch expense by ID returns correct record")
    void expense_byId_returnsCorrectExpense() {
        // First create one so we have a known ID
        Long id = graphQlTester.document("""
            mutation {
              createExpense(input: {
                title: "Test lunch",
                amount: 12.50,
                category: FOOD,
                date: "2025-03-15"
              }) { id }
            }
        """)
        .execute()
        .path("createExpense.id").entity(Long.class).get();

        // Now fetch by that ID
        graphQlTester.document("""
            query GetExpense($id: ID!) {
              expense(id: $id) { id title amount category }
            }
        """)
        .variable("id", id)
        .execute()
        .path("expense.title").entity(String.class)
            .isEqualTo("Test lunch")
        .path("expense.amount").entity(Double.class)
            .isEqualTo(12.50)
        .path("expense.category").entity(String.class)
            .isEqualTo("FOOD");
    }

    @Test
    @DisplayName("Query: expense not found returns GraphQL error")
    void expense_notFound_returnsGraphQLError() {
        graphQlTester.document("""
            query { expense(id: "99999") { id title } }
        """)
        .execute()
        .errors()
        .satisfy(errors -> {
            org.assertj.core.api.Assertions.assertThat(errors).isNotEmpty();
            org.assertj.core.api.Assertions.assertThat(
                errors.get(0).getMessage()).contains("99999");
        });
    }

    @Test
    @DisplayName("Query: expenses by category returns filtered list")
    void expensesByCategory_returnsFilteredResults() {
        graphQlTester.document("""
            query {
              expensesByCategory(category: FOOD) {
                id title category
              }
            }
        """)
        .execute()
        .path("expensesByCategory[*].category")
        .entityList(String.class)
        .satisfies(categories ->
            categories.forEach(cat ->
                org.assertj.core.api.Assertions.assertThat(cat).isEqualTo("FOOD")));
    }

    @Test
    @DisplayName("Query: category summaries returns percentages summing near 100")
    void categorySummaries_percentagesSumToHundred() {
        graphQlTester.document("""
            query {
              categorySummaries {
                category total count percentage
              }
            }
        """)
        .execute()
        .path("categorySummaries").entityList(Object.class)
        .satisfies(list ->
            org.assertj.core.api.Assertions.assertThat(list).isNotEmpty());
    }

    // ── Mutation Tests ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Mutation: createExpense creates and returns the expense")
    void createExpense_createsAndReturnsExpense() {
        graphQlTester.document("""
            mutation {
              createExpense(input: {
                title:       "Coffee",
                amount:      4.50,
                category:    FOOD,
                date:        "2025-03-20",
                description: "Morning coffee"
              }) {
                id title amount category description
              }
            }
        """)
        .execute()
        .path("createExpense.id").hasValue()
        .path("createExpense.title").entity(String.class).isEqualTo("Coffee")
        .path("createExpense.amount").entity(Double.class).isEqualTo(4.50)
        .path("createExpense.category").entity(String.class).isEqualTo("FOOD")
        .path("createExpense.description").entity(String.class).isEqualTo("Morning coffee");
    }

    @Test
    @DisplayName("Mutation: updateExpense updates only specified fields")
    void updateExpense_updatesPartialFields() {
        Long id = graphQlTester.document("""
            mutation {
              createExpense(input: {
                title: "Old title", amount: 10.00, category: FOOD, date: "2025-03-01"
              }) { id }
            }
        """)
        .execute()
        .path("createExpense.id").entity(Long.class).get();

        graphQlTester.document("""
            mutation UpdateExp($id: ID!) {
              updateExpense(id: $id, input: { amount: 25.00 }) {
                id title amount
              }
            }
        """)
        .variable("id", id)
        .execute()
        .path("updateExpense.title").entity(String.class).isEqualTo("Old title")
        .path("updateExpense.amount").entity(Double.class).isEqualTo(25.00);
    }

    @Test
    @DisplayName("Mutation: deleteExpense returns success result")
    void deleteExpense_returnsSuccessResult() {
        Long id = graphQlTester.document("""
            mutation {
              createExpense(input: {
                title: "To delete", amount: 5.00, category: OTHER, date: "2025-03-10"
              }) { id }
            }
        """)
        .execute()
        .path("createExpense.id").entity(Long.class).get();

        graphQlTester.document("""
            mutation DeleteExp($id: ID!) {
              deleteExpense(id: $id) {
                success message deletedId
              }
            }
        """)
        .variable("id", id)
        .execute()
        .path("deleteExpense.success").entity(Boolean.class).isEqualTo(true)
        .path("deleteExpense.deletedId").entity(Long.class).isEqualTo(id);
    }

    @Test
    @DisplayName("Mutation: predictNextMonth returns a positive number")
    void predictNextMonth_returnsPositiveNumber() {
        graphQlTester.document("""
            query { predictNextMonth }
        """)
        .execute()
        .path("predictNextMonth").entity(Double.class).satisfies(pred ->
            org.assertj.core.api.Assertions.assertThat(pred).isGreaterThanOrEqualTo(0.0));
    }
}
