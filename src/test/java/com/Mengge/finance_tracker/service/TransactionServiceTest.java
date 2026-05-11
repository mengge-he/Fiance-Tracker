package com.Mengge.finance_tracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.Mengge.finance_tracker.dto.transaction.TransactionQueryResponse;
import com.Mengge.finance_tracker.dto.transaction.TransactionRequest;
import com.Mengge.finance_tracker.dto.transaction.TransactionResponse;
import com.Mengge.finance_tracker.entity.Transaction;
import com.Mengge.finance_tracker.entity.User;
import com.Mengge.finance_tracker.enums.TransactionType;
import com.Mengge.finance_tracker.exception.ResourceNotFoundException;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setName("User");
        user.setPassword("hash");
    }

    @Test
    void create_savesTransactionForResolvedUser() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionRequest req = new TransactionRequest(
            TransactionType.EXPENSE,
            new BigDecimal("25.00"),
            "  Food ",
            LocalDate.of(2026, 5, 1),
            "lunch"
        );
        TransactionResponse out = transactionService.create("user@example.com", req);

        assertThat(out.id()).isNull();
        assertThat(out.category()).isEqualTo("Food");
        assertThat(out.amount()).isEqualByComparingTo("25.00");
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void getById_throwsWhenNotOwned() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(transactionRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getById("user@example.com", 99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Transaction not found");
    }

    @Test
    void search_throwsWhenFromAfterTo() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(
            () -> transactionService.search(
                "user@example.com",
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 1),
                null,
                null,
                null,
                null,
                "date,desc"
            )
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fromDate");
    }

    @Test
    void search_unpaged_returnsContent() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        Transaction t = Transaction.builder()
            .id(1L)
            .type(TransactionType.INCOME)
            .amount(BigDecimal.ONE)
            .category("Salary")
            .date(LocalDate.of(2026, 5, 1))
            .user(user)
            .build();
        when(transactionRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Sort.class)))
            .thenReturn(List.of(t));

        TransactionQueryResponse resp = transactionService.search(
            "user@example.com", null, null, null, null, null, null, "date,desc"
        );

        assertThat(resp.paged()).isFalse();
        assertThat(resp.content()).hasSize(1);
        assertThat(resp.content().getFirst().type()).isEqualTo(TransactionType.INCOME);
    }

    @Test
    void search_paged_returnsMetadata() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        Transaction t = Transaction.builder()
            .id(1L)
            .type(TransactionType.EXPENSE)
            .amount(new BigDecimal("5.00"))
            .category("X")
            .date(LocalDate.of(2026, 5, 1))
            .user(user)
            .build();
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(t), org.springframework.data.domain.PageRequest.of(0, 20), 1));

        TransactionQueryResponse resp = transactionService.search(
            "user@example.com", null, null, null, null, 0, 20, "date,desc"
        );

        assertThat(resp.paged()).isTrue();
        assertThat(resp.totalElements()).isEqualTo(1L);
        assertThat(resp.totalPages()).isEqualTo(1);
    }

    @Test
    void search_invalidSortField_throws() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(
            () -> transactionService.search(
                "user@example.com", null, null, null, null, null, null, "foo,asc"
            )
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported sort field");
    }
}
