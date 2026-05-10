package com.Mengge.finance_tracker.service;

import com.Mengge.finance_tracker.dto.transaction.TransactionRequest;
import com.Mengge.finance_tracker.dto.transaction.TransactionResponse;
import com.Mengge.finance_tracker.entity.Transaction;
import com.Mengge.finance_tracker.entity.User;
import com.Mengge.finance_tracker.repository.TransactionRepository;
import com.Mengge.finance_tracker.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional
    public TransactionResponse create(String userEmail, TransactionRequest request) {
        User user = requireUser(userEmail);
        Transaction transaction = Transaction.builder()
            .type(request.type())
            .amount(request.amount())
            .category(request.category().trim())
            .date(request.date())
            .note(request.note())
            .user(user)
            .build();
        return toResponse(transactionRepository.save(transaction));
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> list(String userEmail) {
        User user = requireUser(userEmail);
        return transactionRepository.findAllByUserIdOrderByDateDescIdDesc(user.getId())
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(String userEmail, Long id) {
        User user = requireUser(userEmail);
        return toResponse(requireOwnedTransaction(user.getId(), id));
    }

    @Transactional
    public TransactionResponse update(String userEmail, Long id, TransactionRequest request) {
        User user = requireUser(userEmail);
        Transaction existing = requireOwnedTransaction(user.getId(), id);
        existing.setType(request.type());
        existing.setAmount(request.amount());
        existing.setCategory(request.category().trim());
        existing.setDate(request.date());
        existing.setNote(request.note());
        return toResponse(transactionRepository.save(existing));
    }

    @Transactional
    public void delete(String userEmail, Long id) {
        User user = requireUser(userEmail);
        Transaction existing = requireOwnedTransaction(user.getId(), id);
        transactionRepository.delete(existing);
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
    }

    private Transaction requireOwnedTransaction(Long userId, Long transactionId) {
        return transactionRepository.findByIdAndUserId(transactionId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
            transaction.getId(),
            transaction.getType(),
            transaction.getAmount(),
            transaction.getCategory(),
            transaction.getDate(),
            transaction.getNote()
        );
    }
}
