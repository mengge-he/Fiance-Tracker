package com.Mengge.finance_tracker.controller;

import com.Mengge.finance_tracker.dto.budget.BudgetCreateRequest;
import com.Mengge.finance_tracker.dto.budget.BudgetItemResponse;
import com.Mengge.finance_tracker.dto.budget.BudgetMonthlyResponse;
import com.Mengge.finance_tracker.dto.budget.BudgetUpdateRequest;
import com.Mengge.finance_tracker.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {
    private final BudgetService budgetService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetItemResponse create(Authentication authentication, @Valid @RequestBody BudgetCreateRequest request) {
        return budgetService.create(authentication.getName(), request);
    }

    @PutMapping("/{id}")
    public BudgetItemResponse update(
        Authentication authentication,
        @PathVariable Long id,
        @Valid @RequestBody BudgetUpdateRequest request
    ) {
        return budgetService.update(authentication.getName(), id, request);
    }

    @GetMapping
    public BudgetMonthlyResponse getMonthly(
        Authentication authentication,
        @RequestParam int year,
        @RequestParam int month
    ) {
        return budgetService.getMonthly(authentication.getName(), year, month);
    }
}
