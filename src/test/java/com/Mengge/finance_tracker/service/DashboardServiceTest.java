package com.Mengge.finance_tracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.Mengge.finance_tracker.dto.dashboard.DashboardResponse;
import com.Mengge.finance_tracker.entity.Budget;
import com.Mengge.finance_tracker.entity.User;
import com.Mengge.finance_tracker.enums.TransactionType;
import com.Mengge.finance_tracker.repository.BudgetRepository;
import com.Mengge.finance_tracker.repository.CategorySpendSummary;
import com.Mengge.finance_tracker.repository.TransactionRepository;
import com.Mengge.finance_tracker.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(2L);
        user.setEmail("dash@example.com");
    }

    @Test
    void getMonthlyDashboard_throwsWhenOnlyYearProvided() {
        when(userRepository.findByEmail("dash@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> dashboardService.getMonthlyDashboard("dash@example.com", 2026, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Both year and month");
    }

    @Test
    void getMonthlyDashboard_computesOverBudgetFromBudgetsAndSpend() {
        when(userRepository.findByEmail("dash@example.com")).thenReturn(Optional.of(user));
        when(
            transactionRepository.sumExpenseTotalForPeriod(
                eq(2L), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)
            )
        ).thenReturn(new BigDecimal("120.00"));
        when(
            transactionRepository.sumExpenseByCategoryForPeriod(
                eq(2L), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)
            )
        ).thenReturn(
            List.of(
                new CategorySpendSummary() {
                    @Override
                    public String getCategory() {
                        return "Food";
                    }

                    @Override
                    public BigDecimal getTotal() {
                        return new BigDecimal("120.00");
                    }
                }
            )
        );
        Budget budget = Budget.builder()
            .id(1L)
            .year(2026)
            .month(5)
            .category("Food")
            .budgetLimit(new BigDecimal("100.00"))
            .user(user)
            .build();
        when(budgetRepository.findByUserIdAndYearAndMonthOrderByCategoryAsc(2L, 2026, 5)).thenReturn(List.of(budget));

        DashboardResponse d = dashboardService.getMonthlyDashboard("dash@example.com", 2026, 5);

        assertThat(d.year()).isEqualTo(2026);
        assertThat(d.month()).isEqualTo(5);
        assertThat(d.totalSpentThisMonth()).isEqualByComparingTo("120.00");
        assertThat(d.spendByCategory()).hasSize(1);
        assertThat(d.overBudgetCategories()).hasSize(1);
        assertThat(d.overBudgetCategories().getFirst().category()).isEqualTo("Food");
        assertThat(d.overBudgetCategories().getFirst().budgetLimit()).isEqualByComparingTo("100.00");
    }
}
