package com.Mengge.finance_tracker.dto.transaction;

import java.util.List;

public record TransactionQueryResponse(
    List<TransactionResponse> content,
    boolean paged,
    Integer page,
    Integer size,
    Long totalElements,
    Integer totalPages
) {
    public static TransactionQueryResponse unpaged(List<TransactionResponse> content) {
        return new TransactionQueryResponse(content, false, null, null, null, null);
    }

    public static TransactionQueryResponse paged(
        List<TransactionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {
        return new TransactionQueryResponse(content, true, page, size, totalElements, totalPages);
    }
}
