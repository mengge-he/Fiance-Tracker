package com.Mengge.finance_tracker.service;

import com.Mengge.finance_tracker.dto.transaction.TransactionQueryResponse;
import com.Mengge.finance_tracker.dto.transaction.TransactionRequest;
import com.Mengge.finance_tracker.dto.transaction.TransactionResponse;
import com.Mengge.finance_tracker.entity.Transaction;
import com.Mengge.finance_tracker.entity.User;
import com.Mengge.finance_tracker.enums.TransactionType;
import com.Mengge.finance_tracker.exception.ResourceNotFoundException;
import com.Mengge.finance_tracker.exception.UnauthorizedException;
import com.Mengge.finance_tracker.util.EmailUtil;
import com.Mengge.finance_tracker.repository.TransactionRepository;
import com.Mengge.finance_tracker.repository.TransactionSpecifications;
import com.Mengge.finance_tracker.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
    public TransactionQueryResponse search(
        String userEmail,
        LocalDate fromDate,
        LocalDate toDate,
        String category,
        TransactionType type,
        Integer page,
        Integer size,
        String sort
    ) {
        User user = requireUser(userEmail);
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate must not be after toDate");
        }
        Specification<Transaction> spec = TransactionSpecifications.ownedByWithFilters(
            user.getId(),
            fromDate,
            toDate,
            category,
            type
        );
        Sort sortSpec = parseSort(sort);
        if (page == null) {
            List<TransactionResponse> items = transactionRepository.findAll(spec, sortSpec).stream()
                .map(this::toResponse)
                .toList();
            return TransactionQueryResponse.unpaged(items);
        }
        int pageSize = size != null ? size : 20;
        if (page < 0 || pageSize < 1) {
            throw new IllegalArgumentException("Invalid page or size");
        }
        Pageable pageable = PageRequest.of(page, pageSize, sortSpec);
        Page<Transaction> result = transactionRepository.findAll(spec, pageable);
        return TransactionQueryResponse.paged(
            result.stream().map(this::toResponse).toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    private static Sort parseSort(String sortParam) {
        String raw = sortParam != null && !sortParam.isBlank() ? sortParam : "date,desc";
        String[] parts = raw.split(",", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("sort must be field,direction (e.g. date,desc)");
        }
        String field = validateSortField(parts[0].trim());
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(parts[1].trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid sort direction");
        }
        return Sort.by(new Sort.Order(direction, field), new Sort.Order(Sort.Direction.DESC, "id"));
    }

    private static String validateSortField(String field) {
        return switch (field) {
            case "date", "amount", "category", "type", "id" -> field;
            default -> throw new IllegalArgumentException("Unsupported sort field: " + field);
        };
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
        String normalized = EmailUtil.normalize(email);
        return userRepository.findByEmail(normalized)
            .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));
    }

    private Transaction requireOwnedTransaction(Long userId, Long transactionId) {
        return transactionRepository.findByIdAndUserId(transactionId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
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
