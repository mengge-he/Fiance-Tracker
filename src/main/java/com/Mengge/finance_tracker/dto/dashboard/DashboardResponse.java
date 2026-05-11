package com.Mengge.finance_tracker.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
    int year,
    int month,
    BigDecimal totalSpentThisMonth,
    List<CategorySpendRow> spendByCategory,
    List<OverBudgetCategoryRow> overBudgetCategories
) {}
