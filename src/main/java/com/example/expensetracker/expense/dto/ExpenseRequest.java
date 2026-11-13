package com.example.expensetracker.expense.dto;

import com.example.expensetracker.expense.Category;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(
        @NotBlank @Size(max = 255) String description,
        @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotNull Category category,
        @NotNull @PastOrPresent LocalDate date
) {
}