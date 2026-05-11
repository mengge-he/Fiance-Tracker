package com.Mengge.finance_tracker.service;

import com.Mengge.finance_tracker.dto.dashboard.CategorySpendRow;
import com.Mengge.finance_tracker.dto.dashboard.DashboardResponse;
import com.Mengge.finance_tracker.dto.dashboard.OverBudgetCategoryRow;
import com.Mengge.finance_tracker.entity.Budget;
import com.Mengge.finance_tracker.entity.User;
import com.Mengge.finance_tracker.enums.TransactionType;
import com.Mengge.finance_tracker.repository.BudgetRepository;
import com.Mengge.finance_tracker.repository.CategorySpendSummary;
import com.Mengge.finance_tracker.repository.TransactionRepository;
import com.Mengge.finance_tracker.repository.UserRepository;
import com.Mengge.finance_tracker.util.CategoryKeyUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getMonthlyDashboard(String userEmail, Integer year, Integer month) {
        User user = requireUser(userEmail);
        YearMonth ym = resolveYearMonth(year, month);
        int y = ym.getYear();
        int m = ym.getMonthValue();
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        BigDecimal totalSpent = transactionRepository.sumExpenseTotalForPeriod(
            user.getId(),
            TransactionType.EXPENSE,
            start,
            end
        );
        if (totalSpent == null) {
            totalSpent = BigDecimal.ZERO;
        }

        List<CategorySpendSummary> rows = transactionRepository.sumExpenseByCategoryForPeriod(
            user.getId(),
            TransactionType.EXPENSE,
            start,
            end
        );
        Map<String, BigDecimal> spendByKey = new HashMap<>();
        Map<String, String> displayCategoryByKey = new HashMap<>();
        for (CategorySpendSummary row : rows) {
            String key = CategoryKeyUtil.normalizeKey(row.getCategory());
            spendByKey.merge(key, row.getTotal(), BigDecimal::add);
            displayCategoryByKey.putIfAbsent(key, row.getCategory());
        }
        List<CategorySpendRow> spendByCategory = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : spendByKey.entrySet()) {
            spendByCategory.add(new CategorySpendRow(displayCategoryByKey.get(e.getKey()), e.getValue()));
        }
        spendByCategory.sort(Comparator.comparing(CategorySpendRow::category, String.CASE_INSENSITIVE_ORDER));

        List<Budget> budgets = budgetRepository.findByUserIdAndYearAndMonthOrderByCategoryAsc(user.getId(), y, m);
        List<OverBudgetCategoryRow> overBudget = new ArrayList<>();
        for (Budget b : budgets) {
            BigDecimal spent = spendByKey.getOrDefault(CategoryKeyUtil.normalizeKey(b.getCategory()), BigDecimal.ZERO);
            if (spent.compareTo(b.getBudgetLimit()) > 0) {
                overBudget.add(new OverBudgetCategoryRow(b.getCategory(), spent, b.getBudgetLimit()));
            }
        }

        return new DashboardResponse(y, m, totalSpent, spendByCategory, overBudget);
    }

    private YearMonth resolveYearMonth(Integer year, Integer month) {
        if (year == null && month == null) {
            return YearMonth.now();
        }
        if (year == null || month == null) {
            throw new IllegalArgumentException("Both year and month are required, or omit both for the current month");
        }
        try {
            return YearMonth.of(year, month);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid year or month");
        }
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
    }
}
