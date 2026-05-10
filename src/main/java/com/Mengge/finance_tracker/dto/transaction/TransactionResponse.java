package com.Mengge.finance_tracker.dto.transaction;

import com.Mengge.finance_tracker.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(
    Long id,
    TransactionType type,
    BigDecimal amount,
    String category,
    LocalDate date,
    String note
) {}
