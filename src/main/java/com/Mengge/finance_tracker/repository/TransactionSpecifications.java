package com.Mengge.finance_tracker.repository;

import com.Mengge.finance_tracker.entity.Transaction;
import com.Mengge.finance_tracker.enums.TransactionType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class TransactionSpecifications {
    private TransactionSpecifications() {}

    public static Specification<Transaction> ownedByWithFilters(
        Long userId,
        LocalDate fromDate,
        LocalDate toDate,
        String category,
        TransactionType type
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("date"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("date"), toDate));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(
                    cb.equal(
                        cb.lower(root.get("category")),
                        category.trim().toLowerCase(Locale.ROOT)
                    )
                );
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
