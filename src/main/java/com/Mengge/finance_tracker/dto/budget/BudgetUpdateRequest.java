package com.Mengge.finance_tracker.dto.budget;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record BudgetUpdateRequest(
    @NotBlank String category,
    @NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal limitAmount
) {}
