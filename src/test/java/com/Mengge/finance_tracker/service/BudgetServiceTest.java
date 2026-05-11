package com.Mengge.finance_tracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.Mengge.finance_tracker.dto.budget.BudgetCreateRequest;
import com.Mengge.finance_tracker.dto.budget.BudgetItemResponse;
import com.Mengge.finance_tracker.dto.budget.BudgetMonthlyResponse;
import com.Mengge.finance_tracker.dto.budget.BudgetUpdateRequest;
import com.Mengge.finance_tracker.entity.Budget;
import com.Mengge.finance_tracker.entity.User;
import com.Mengge.finance_tracker.enums.TransactionType;
import com.Mengge.finance_tracker.exception.ResourceNotFoundException;
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
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BudgetService budgetService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("u@example.com");
    }

    @Test
    void create_throwsWhenDuplicateCategoryMonth() {
        when(userRepository.findByEmail("u@example.com")).thenReturn(Optional.of(user));
        when(budgetRepository.existsByUserIdAndYearAndMonthAndCategory(1L, 2026, 5, "Food")).thenReturn(true);

        BudgetCreateRequest req = new BudgetCreateRequest(2026, 5, "Food", new BigDecimal("100.00"));

        assertThatThrownBy(() -> budgetService.create("u@example.com", req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void create_setsOverBudgetWhenSpendExceedsLimit() {
        when(userRepository.findByEmail("u@example.com")).thenReturn(Optional.of(user));
        when(budgetRepository.existsByUserIdAndYearAndMonthAndCategory(1L, 2026, 5, "Food")).thenReturn(false);
        when(transactionRepository.sumExpenseByCategoryForPeriod(
            eq(1L), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)
        )).thenReturn(
            List.of(
                new CategorySpendSummary() {
                    @Override
                    public String getCategory() {
                        return "Food";
                    }

                    @Override
                    public BigDecimal getTotal() {
                        return new BigDecimal("150.00");
                    }
                }
            )
        );

        BudgetCreateRequest req = new BudgetCreateRequest(2026, 5, "Food", new BigDecimal("100.00"));
        BudgetItemResponse out = budgetService.create("u@example.com", req);

        assertThat(out.overBudget()).isTrue();
        assertThat(out.spent()).isEqualByComparingTo("150.00");
        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    void getMonthly_throwsOnInvalidMonth() {
        when(userRepository.findByEmail("u@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> budgetService.getMonthly("u@example.com", 2026, 13))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid");
    }

    @Test
    void update_throwsWhenBudgetNotOwned() {
        when(userRepository.findByEmail("u@example.com")).thenReturn(Optional.of(user));
        when(budgetRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> budgetService.update("u@example.com", 5L, new BudgetUpdateRequest("Food", new BigDecimal("50.00")))
        ).isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Budget not found");
    }

    @Test
    void getMonthly_returnsItemsWithSpend() {
        when(userRepository.findByEmail("u@example.com")).thenReturn(Optional.of(user));
        when(transactionRepository.sumExpenseByCategoryForPeriod(
            eq(1L), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)
        )).thenReturn(List.of());
        Budget b = Budget.builder()
            .id(1L)
            .year(2026)
            .month(5)
            .category("Rent")
            .budgetLimit(new BigDecimal("800.00"))
            .user(user)
            .build();
        when(budgetRepository.findByUserIdAndYearAndMonthOrderByCategoryAsc(1L, 2026, 5)).thenReturn(List.of(b));

        BudgetMonthlyResponse resp = budgetService.getMonthly("u@example.com", 2026, 5);

        assertThat(resp.year()).isEqualTo(2026);
        assertThat(resp.month()).isEqualTo(5);
        assertThat(resp.budgets()).hasSize(1);
        assertThat(resp.budgets().getFirst().overBudget()).isFalse();
    }
}
