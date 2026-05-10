package com.Mengge.finance_tracker.repository;

import com.Mengge.finance_tracker.entity.Transaction;
import com.Mengge.finance_tracker.enums.TransactionType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findAllByUserIdOrderByDateDescIdDesc(Long userId);
    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    @Query("""
        select t.category as category, coalesce(sum(t.amount), 0) as total
        from Transaction t
        where t.user.id = :userId
          and t.type = :expenseType
          and t.date between :startInclusive and :endInclusive
        group by t.category
        """)
    List<CategorySpendSummary> sumExpenseByCategoryForPeriod(
        @Param("userId") Long userId,
        @Param("expenseType") TransactionType expenseType,
        @Param("startInclusive") LocalDate startInclusive,
        @Param("endInclusive") LocalDate endInclusive
    );
}
