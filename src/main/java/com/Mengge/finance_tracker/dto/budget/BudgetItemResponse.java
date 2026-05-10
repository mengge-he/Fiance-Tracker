package com.Mengge.finance_tracker.dto.budget;

import java.math.BigDecimal;

public record BudgetItemResponse(
    Long id,
    int year,
    int month,
    String category,
    BigDecimal limitAmount,
    BigDecimal spent,
    boolean overBudget
) {}
