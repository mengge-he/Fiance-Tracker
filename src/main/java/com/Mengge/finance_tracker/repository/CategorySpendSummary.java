package com.Mengge.finance_tracker.repository;

import java.math.BigDecimal;

public interface CategorySpendSummary {
    String getCategory();

    BigDecimal getTotal();
}
