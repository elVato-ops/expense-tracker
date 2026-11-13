package com.example.expensetracker.expense;

import com.example.expensetracker.expense.dto.ExpenseRequest;
import com.example.expensetracker.expense.dto.ExpenseResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 1);

    @Mock
    private ExpenseRepository repository;

    @InjectMocks
    private ExpenseService service;

    @Test
    void findAll_withoutCategory_returnsAllExpenses() {
        when(repository.findAll()).thenReturn(List.of(
                expense(1L, "Lunch", "12.50", Category.FOOD),
                expense(2L, "Bus ticket", "3.20", Category.TRANSPORT)));

        List<ExpenseResponse> result = service.findAll(null);

        assertThat(result).extracting(ExpenseResponse::id).containsExactly(1L, 2L);
        verify(repository, never()).findByCategory(any());
    }

    @Test
    void findAll_withCategory_filtersByCategory() {
        when(repository.findByCategory(Category.FOOD))
                .thenReturn(List.of(expense(1L, "Lunch", "12.50", Category.FOOD)));

        List<ExpenseResponse> result = service.findAll(Category.FOOD);

        assertThat(result).singleElement()
                .satisfies(r -> assertThat(r.category()).isEqualTo(Category.FOOD));
        verify(repository, never()).findAll();
    }

    @Test
    void findById_existing_returnsExpense() {
        when(repository.findById(1L)).thenReturn(Optional.of(expense(1L, "Lunch", "12.50", Category.FOOD)));

        ExpenseResponse result = service.findById(1L);

        assertThat(result).isEqualTo(
                new ExpenseResponse(1L, "Lunch", new BigDecimal("12.50"), Category.FOOD, DATE));
    }

    @Test
    void findById_missing_throwsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ExpenseNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_savesAndReturnsExpense() {
        ExpenseRequest request = new ExpenseRequest("Lunch", new BigDecimal("12.50"), Category.FOOD, DATE);
        when(repository.save(any(Expense.class))).thenAnswer(invocation -> {
            Expense saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });

        ExpenseResponse result = service.create(request);

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getDescription()).isEqualTo("Lunch");
        assertThat(result).isEqualTo(
                new ExpenseResponse(1L, "Lunch", new BigDecimal("12.50"), Category.FOOD, DATE));
    }

    @Test
    void update_existing_updatesFields() {
        Expense existing = expense(1L, "Lunch", "12.50", Category.FOOD);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        ExpenseRequest request = new ExpenseRequest("Taxi", new BigDecimal("40.00"), Category.TRANSPORT, DATE);

        ExpenseResponse result = service.update(1L, request);

        assertThat(result).isEqualTo(
                new ExpenseResponse(1L, "Taxi", new BigDecimal("40.00"), Category.TRANSPORT, DATE));
        assertThat(existing.getDescription()).isEqualTo("Taxi");
    }

    @Test
    void update_missing_throwsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        ExpenseRequest request = new ExpenseRequest("Taxi", new BigDecimal("40.00"), Category.TRANSPORT, DATE);

        assertThatThrownBy(() -> service.update(99L, request))
                .isInstanceOf(ExpenseNotFoundException.class);
    }

    @Test
    void delete_existing_deletes() {
        when(repository.existsById(1L)).thenReturn(true);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void delete_missing_throwsNotFound() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ExpenseNotFoundException.class);
        verify(repository, never()).deleteById(anyLong());
    }

    private static Expense expense(Long id, String description, String amount, Category category) {
        Expense expense = new Expense(description, new BigDecimal(amount), category, DATE);
        ReflectionTestUtils.setField(expense, "id", id);
        return expense;
    }
}
