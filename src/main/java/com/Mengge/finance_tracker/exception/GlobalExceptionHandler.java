package com.Mengge.finance_tracker.exception;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(
            Map.of(
                "timestamp", Instant.now().toString(),
                "error", "Bad Request",
                "message", ex.getMessage()
            )
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            Map.of(
                "timestamp", Instant.now().toString(),
                "error", "Internal Server Error",
                "message", ex.getMessage()
            )
        );
    }
}
