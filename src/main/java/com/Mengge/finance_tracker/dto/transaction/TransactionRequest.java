package com.Mengge.finance_tracker.dto.transaction;

import com.Mengge.finance_tracker.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
    @NotNull TransactionType type,
    @NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal amount,
    @NotBlank String category,
    @NotNull LocalDate date,
    String note
) {}
