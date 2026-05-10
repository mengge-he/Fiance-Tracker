package com.Mengge.finance_tracker.repository;

import com.Mengge.finance_tracker.entity.Transaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findAllByUserIdOrderByDateDescIdDesc(Long userId);
    Optional<Transaction> findByIdAndUserId(Long id, Long userId);
}
