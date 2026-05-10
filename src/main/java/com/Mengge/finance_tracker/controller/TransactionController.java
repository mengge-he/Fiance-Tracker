package com.Mengge.finance_tracker.controller;

import com.Mengge.finance_tracker.dto.transaction.TransactionRequest;
import com.Mengge.finance_tracker.dto.transaction.TransactionResponse;
import com.Mengge.finance_tracker.service.TransactionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(
        Authentication authentication,
        @Valid @RequestBody TransactionRequest request
    ) {
        return transactionService.create(authentication.getName(), request);
    }

    @GetMapping
    public List<TransactionResponse> list(Authentication authentication) {
        return transactionService.list(authentication.getName());
    }

    @GetMapping("/{id}")
    public TransactionResponse getById(Authentication authentication, @PathVariable Long id) {
        return transactionService.getById(authentication.getName(), id);
    }

    @PutMapping("/{id}")
    public TransactionResponse update(
        Authentication authentication,
        @PathVariable Long id,
        @Valid @RequestBody TransactionRequest request
    ) {
        return transactionService.update(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        transactionService.delete(authentication.getName(), id);
    }
}
