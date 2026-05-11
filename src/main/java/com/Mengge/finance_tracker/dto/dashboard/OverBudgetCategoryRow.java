package com.Mengge.finance_tracker.dto.dashboard;

import java.math.BigDecimal;

public record OverBudgetCategoryRow(String category, BigDecimal spent, BigDecimal budgetLimit) {}
