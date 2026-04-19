package com.expensetracker;

import com.expensetracker.exception.ExpenseNotFoundException;
import com.expensetracker.model.Category;
import com.expensetracker.model.Dtos.*;
import com.expensetracker.model.Expense;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.service.ExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExpenseService using Mockito.
 * No Spring context is loaded — tests are fast and isolated.
 */
@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private ExpenseService expenseService;

    private Expense sampleExpense;

    @BeforeEach
    void setUp() {
        sampleExpense = Expense.builder()
                .id(1L)
                .title("Team lunch")
                .amount(48.50)
                .category(Category.FOOD)
                .date(LocalDate.of(2025, 3, 15))
                .description("Monthly team lunch")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("returns expense when found")
        void returnsExpenseWhenFound() {
            when(expenseRepository.findById(1L)).thenReturn(Optional.of(sampleExpense));

            Expense result = expenseService.findById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("Team lunch");
            assertThat(result.getAmount()).isEqualTo(48.50);
            verify(expenseRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("throws ExpenseNotFoundException when not found")
        void throwsWhenNotFound() {
            when(expenseRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseService.findById(999L))
                    .isInstanceOf(ExpenseNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    // ── createExpense ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createExpense")
    class CreateExpense {

        @Test
        @DisplayName("saves and returns the new expense")
        void savesAndReturnsExpense() {
            CreateExpenseInput input = new CreateExpenseInput(
                    "Coffee", 4.50, Category.FOOD, "2025-03-20", "Morning coffee");

            when(expenseRepository.save(any(Expense.class))).thenReturn(sampleExpense);

            Expense result = expenseService.createExpense(input);

            assertThat(result).isNotNull();
            verify(expenseRepository, times(1)).save(any(Expense.class));
        }

        @Test
        @DisplayName("maps all input fields onto the entity correctly")
        void mapsInputFieldsCorrectly() {
            CreateExpenseInput input = new CreateExpenseInput(
                    "Gym", 40.00, Category.HEALTH, "2025-03-01", "Monthly gym");

            Expense captured = Expense.builder()
                    .title("Gym").amount(40.00).category(Category.HEALTH)
                    .date(LocalDate.of(2025, 3, 1)).description("Monthly gym").build();

            when(expenseRepository.save(any(Expense.class))).thenReturn(captured);

            Expense result = expenseService.createExpense(input);

            assertThat(result.getTitle()).isEqualTo("Gym");
            assertThat(result.getCategory()).isEqualTo(Category.HEALTH);
        }
    }

    // ── updateExpense ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateExpense")
    class UpdateExpense {

        @Test
        @DisplayName("updates only the provided non-null fields")
        void updatesOnlyNonNullFields() {
            UpdateExpenseInput input = new UpdateExpenseInput(null, 99.99, null, null, null);

            when(expenseRepository.findById(1L)).thenReturn(Optional.of(sampleExpense));
            when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

            Expense result = expenseService.updateExpense(1L, input);

            // Amount should change
            assertThat(result.getAmount()).isEqualTo(99.99);
            // Title stays the same (input.title() was null)
            assertThat(result.getTitle()).isEqualTo("Team lunch");
        }

        @Test
        @DisplayName("throws when expense to update is not found")
        void throwsWhenExpenseNotFound() {
            when(expenseRepository.findById(55L)).thenReturn(Optional.empty());

            UpdateExpenseInput input = new UpdateExpenseInput("New title", null, null, null, null);

            assertThatThrownBy(() -> expenseService.updateExpense(55L, input))
                    .isInstanceOf(ExpenseNotFoundException.class);
        }
    }

    // ── deleteExpense ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteExpense")
    class DeleteExpense {

        @Test
        @DisplayName("deletes and returns a successful DeleteResult")
        void deletesAndReturnsSuccessResult() {
            when(expenseRepository.findById(1L)).thenReturn(Optional.of(sampleExpense));
            doNothing().when(expenseRepository).deleteById(1L);

            DeleteResult result = expenseService.deleteExpense(1L);

            assertThat(result.success()).isTrue();
            assertThat(result.deletedId()).isEqualTo(1L);
            assertThat(result.message()).contains("Team lunch");
            verify(expenseRepository).deleteById(1L);
        }

        @Test
        @DisplayName("throws when expense to delete is not found")
        void throwsWhenNotFound() {
            when(expenseRepository.findById(77L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseService.deleteExpense(77L))
                    .isInstanceOf(ExpenseNotFoundException.class)
                    .hasMessageContaining("77");

            verify(expenseRepository, never()).deleteById(any());
        }
    }

    // ── findExpenses (paged) ──────────────────────────────────────────────────

    @Nested
    @DisplayName("findExpenses")
    class FindExpenses {

        @Test
        @DisplayName("returns paged result with correct metadata")
        @SuppressWarnings("unchecked")
        void returnsPaged() {
            List<Expense> expenses = List.of(sampleExpense);
            var page = new PageImpl<>(expenses);

            when(expenseRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(page);

            ExpensePage result = expenseService.findExpenses(null, null);

            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.content()).hasSize(1);
            assertThat(result.currentPage()).isEqualTo(0);
        }
    }

    // ── predictNextMonth ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("predictNextMonth")
    class PredictNextMonth {

        @Test
        @DisplayName("returns a non-negative prediction")
        void returnsNonNegativePrediction() {
            when(expenseRepository.sumByDateRange(any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(300.0);

            Double prediction = expenseService.predictNextMonth();

            assertThat(prediction).isGreaterThanOrEqualTo(0.0);
        }

        @Test
        @DisplayName("returns 0.0 when no expense data exists")
        void returnsZeroWhenNoData() {
            when(expenseRepository.sumByDateRange(any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(0.0);

            Double prediction = expenseService.predictNextMonth();

            assertThat(prediction).isEqualTo(0.0);
        }
    }
}
