package com.Mengge.finance_tracker.controller;

import com.Mengge.finance_tracker.dto.transaction.TransactionQueryResponse;
import com.Mengge.finance_tracker.dto.transaction.TransactionRequest;
import com.Mengge.finance_tracker.dto.transaction.TransactionResponse;
import com.Mengge.finance_tracker.enums.TransactionType;
import com.Mengge.finance_tracker.service.TransactionService;
import jakarta.validation.Valid;
import java.time.LocalDate;
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
import org.springframework.web.bind.annotation.RequestParam;
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
    public TransactionQueryResponse list(
        Authentication authentication,
        @RequestParam(required = false) LocalDate fromDate,
        @RequestParam(required = false) LocalDate toDate,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) TransactionType type,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false, defaultValue = "date,desc") String sort
    ) {
        return transactionService.search(
            authentication.getName(),
            fromDate,
            toDate,
            category,
            type,
            page,
            size,
            sort
        );
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
