package com.Mengge.finance_tracker.dto.budget;

import java.util.List;

public record BudgetMonthlyResponse(int year, int month, List<BudgetItemResponse> budgets) {}
