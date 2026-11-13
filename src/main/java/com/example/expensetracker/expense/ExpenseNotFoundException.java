package com.example.expensetracker.expense;

public class ExpenseNotFoundException extends RuntimeException {

    public ExpenseNotFoundException(Long id) {
        super("Expense with id " + id + " not found");
    }
}