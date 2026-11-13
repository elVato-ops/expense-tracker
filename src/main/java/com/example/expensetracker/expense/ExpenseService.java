package com.example.expensetracker.expense;

import com.example.expensetracker.expense.dto.ExpenseRequest;
import com.example.expensetracker.expense.dto.ExpenseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseService {

    private final ExpenseRepository repository;

    public List<ExpenseResponse> findAll(Category category) {
        List<Expense> expenses = category == null
                ? repository.findAll()
                : repository.findByCategory(category);
        return expenses.stream().map(ExpenseResponse::from).toList();
    }

    public ExpenseResponse findById(Long id) {
        return ExpenseResponse.from(getOrThrow(id));
    }

    @Transactional
    public ExpenseResponse create(ExpenseRequest request) {
        Expense expense = new Expense(
                request.description(),
                request.amount(),
                request.category(),
                request.date()
        );
        return ExpenseResponse.from(repository.save(expense));
    }

    @Transactional
    public ExpenseResponse update(Long id, ExpenseRequest request) {
        Expense expense = getOrThrow(id);
        expense.setDescription(request.description());
        expense.setAmount(request.amount());
        expense.setCategory(request.category());
        expense.setDate(request.date());
        return ExpenseResponse.from(expense);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ExpenseNotFoundException(id);
        }
        repository.deleteById(id);
    }

    private Expense getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new ExpenseNotFoundException(id));
    }
}