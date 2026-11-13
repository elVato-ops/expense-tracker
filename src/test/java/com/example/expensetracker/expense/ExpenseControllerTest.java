package com.example.expensetracker.expense;

import com.example.expensetracker.expense.dto.ExpenseRequest;
import com.example.expensetracker.expense.dto.ExpenseResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExpenseController.class)
class ExpenseControllerTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 1);

    private static final String VALID_BODY = """
            {
              "description": "Lunch",
              "amount": 12.50,
              "category": "FOOD",
              "date": "2026-11-01"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExpenseService service;

    @Test
    void getAll_returnsList() throws Exception {
        when(service.findAll(null)).thenReturn(List.of(response(1L)));

        mockMvc.perform(get("/api/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].description").value("Lunch"))
                .andExpect(jsonPath("$[0].amount").value(12.50))
                .andExpect(jsonPath("$[0].category").value("FOOD"))
                .andExpect(jsonPath("$[0].date").value("2026-11-01"));
    }

    @Test
    void getAll_withCategory_passesFilterToService() throws Exception {
        when(service.findAll(Category.FOOD)).thenReturn(List.of(response(1L)));

        mockMvc.perform(get("/api/expenses").param("category", "FOOD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("FOOD"));

        verify(service).findAll(Category.FOOD);
    }

    @Test
    void getAll_withUnknownCategory_returns400() throws Exception {
        mockMvc.perform(get("/api/expenses").param("category", "CARS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid value 'CARS' for parameter 'category'"));

        verifyNoInteractions(service);
    }

    @Test
    void getById_existing_returns200() throws Exception {
        when(service.findById(1L)).thenReturn(response(1L));

        mockMvc.perform(get("/api/expenses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getById_missing_returns404() throws Exception {
        when(service.findById(99L)).thenThrow(new ExpenseNotFoundException(99L));

        mockMvc.perform(get("/api/expenses/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Expense with id 99 not found"));
    }

    @Test
    void getById_nonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/api/expenses/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_valid_returns201WithLocation() throws Exception {
        when(service.create(any(ExpenseRequest.class))).thenReturn(response(1L));

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/expenses/1"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void create_invalid_returns400WithFieldErrors() throws Exception {
        String body = """
                {
                  "description": "",
                  "amount": -5,
                  "date": "2999-01-01"
                }
                """;

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.amount").exists())
                .andExpect(jsonPath("$.errors.category").exists())
                .andExpect(jsonPath("$.errors.date").exists());

        verifyNoInteractions(service);
    }

    @Test
    void create_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"description\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Malformed request body"));
    }

    @Test
    void create_unknownCategoryInBody_returns400() throws Exception {
        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("FOOD", "CARS")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_existing_returns200() throws Exception {
        when(service.update(eq(1L), any(ExpenseRequest.class))).thenReturn(response(1L));

        mockMvc.perform(put("/api/expenses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void update_missing_returns404() throws Exception {
        when(service.update(eq(99L), any(ExpenseRequest.class))).thenThrow(new ExpenseNotFoundException(99L));

        mockMvc.perform(put("/api/expenses/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_existing_returns204() throws Exception {
        mockMvc.perform(delete("/api/expenses/1"))
                .andExpect(status().isNoContent());

        verify(service).delete(1L);
    }

    @Test
    void delete_missing_returns404() throws Exception {
        doThrow(new ExpenseNotFoundException(99L)).when(service).delete(99L);

        mockMvc.perform(delete("/api/expenses/99"))
                .andExpect(status().isNotFound());
    }

    private static ExpenseResponse response(Long id) {
        return new ExpenseResponse(id, "Lunch", new BigDecimal("12.50"), Category.FOOD, DATE);
    }
}
