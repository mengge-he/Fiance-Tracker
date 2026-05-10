package com.Mengge.finance_tracker.dto.budget;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record BudgetCreateRequest(
    @NotNull @Min(1900) @Max(2100) Integer year,
    @NotNull @Min(1) @Max(12) Integer month,
    @NotBlank String category,
    @NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal limitAmount
) {}
