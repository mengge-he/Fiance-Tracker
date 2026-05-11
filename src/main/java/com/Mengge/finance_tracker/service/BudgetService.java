package com.Mengge.finance_tracker.service;

import com.Mengge.finance_tracker.dto.budget.BudgetCreateRequest;
import com.Mengge.finance_tracker.dto.budget.BudgetItemResponse;
import com.Mengge.finance_tracker.dto.budget.BudgetMonthlyResponse;
import com.Mengge.finance_tracker.dto.budget.BudgetUpdateRequest;
import com.Mengge.finance_tracker.entity.Budget;
import com.Mengge.finance_tracker.entity.User;
import com.Mengge.finance_tracker.enums.TransactionType;
import com.Mengge.finance_tracker.exception.ResourceNotFoundException;
import com.Mengge.finance_tracker.exception.UnauthorizedException;
import com.Mengge.finance_tracker.util.EmailUtil;
import com.Mengge.finance_tracker.repository.BudgetRepository;
import com.Mengge.finance_tracker.repository.CategorySpendSummary;
import com.Mengge.finance_tracker.repository.TransactionRepository;
import com.Mengge.finance_tracker.repository.UserRepository;
import com.Mengge.finance_tracker.util.CategoryKeyUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BudgetService {
    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional
    public BudgetItemResponse create(String userEmail, BudgetCreateRequest request) {
        User user = requireUser(userEmail);
        int year = request.year();
        int month = request.month();
        String category = request.category().trim();
        if (category.isEmpty()) {
            throw new IllegalArgumentException("Category is required");
        }
        if (budgetRepository.existsByUserIdAndYearAndMonthAndCategory(user.getId(), year, month, category)) {
            throw new IllegalArgumentException("Budget already exists for this month and category");
        }
        Budget budget = Budget.builder()
            .year(year)
            .month(month)
            .category(category)
            .budgetLimit(request.limitAmount())
            .user(user)
            .build();
        budgetRepository.save(budget);
        return toItemWithSpend(budget, spendByCategoryKey(user.getId(), year, month));
    }

    @Transactional
    public BudgetItemResponse update(String userEmail, Long id, BudgetUpdateRequest request) {
        User user = requireUser(userEmail);
        Budget budget = requireOwnedBudget(user.getId(), id);
        String category = request.category().trim();
        if (category.isEmpty()) {
            throw new IllegalArgumentException("Category is required");
        }
        int year = budget.getYear();
        int month = budget.getMonth();
        if (!category.equals(budget.getCategory())
            && budgetRepository.existsByUserIdAndYearAndMonthAndCategoryAndIdNot(
                user.getId(), year, month, category, id
            )) {
            throw new IllegalArgumentException("Budget already exists for this month and category");
        }
        budget.setCategory(category);
        budget.setBudgetLimit(request.limitAmount());
        budgetRepository.save(budget);
        return toItemWithSpend(budget, spendByCategoryKey(user.getId(), year, month));
    }

    @Transactional(readOnly = true)
    public BudgetMonthlyResponse getMonthly(String userEmail, int year, int month) {
        User user = requireUser(userEmail);
        validateYearMonth(year, month);
        Map<String, BigDecimal> spendKeys = spendByCategoryKey(user.getId(), year, month);
        List<BudgetItemResponse> items = budgetRepository
            .findByUserIdAndYearAndMonthOrderByCategoryAsc(user.getId(), year, month)
            .stream()
            .map(b -> toItemWithSpend(b, spendKeys))
            .toList();
        return new BudgetMonthlyResponse(year, month, items);
    }

    @Transactional
    public void delete(String userEmail, Long id) {
        User user = requireUser(userEmail);
        Budget budget = requireOwnedBudget(user.getId(), id);
        budgetRepository.delete(budget);
    }

    private void validateYearMonth(int year, int month) {
        try {
            YearMonth.of(year, month);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid year or month");
        }
    }

    private User requireUser(String email) {
        String normalized = EmailUtil.normalize(email);
        return userRepository.findByEmail(normalized)
            .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));
    }

    private Budget requireOwnedBudget(Long userId, Long budgetId) {
        return budgetRepository.findByIdAndUserId(budgetId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
    }

    private Map<String, BigDecimal> spendByCategoryKey(Long userId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        List<CategorySpendSummary> rows = transactionRepository.sumExpenseByCategoryForPeriod(
            userId,
            TransactionType.EXPENSE,
            start,
            end
        );
        Map<String, BigDecimal> map = new HashMap<>();
        for (CategorySpendSummary row : rows) {
            String key = CategoryKeyUtil.normalizeKey(row.getCategory());
            map.merge(key, row.getTotal(), BigDecimal::add);
        }
        return map;
    }

    private BudgetItemResponse toItemWithSpend(Budget budget, Map<String, BigDecimal> spendByCategoryKey) {
        BigDecimal spent = spendByCategoryKey.getOrDefault(
            CategoryKeyUtil.normalizeKey(budget.getCategory()),
            BigDecimal.ZERO
        );
        BigDecimal limit = budget.getBudgetLimit();
        boolean over = spent.compareTo(limit) > 0;
        return new BudgetItemResponse(
            budget.getId(),
            budget.getYear(),
            budget.getMonth(),
            budget.getCategory(),
            limit,
            spent,
            over
        );
    }
}
