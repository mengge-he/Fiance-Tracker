package com.Mengge.finance_tracker.repository;

import com.Mengge.finance_tracker.entity.Budget;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserIdAndYearAndMonthOrderByCategoryAsc(Long userId, int year, int month);

    Optional<Budget> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndYearAndMonthAndCategory(Long userId, int year, int month, String category);

    boolean existsByUserIdAndYearAndMonthAndCategoryAndIdNot(
        Long userId, int year, int month, String category, Long id
    );
}
