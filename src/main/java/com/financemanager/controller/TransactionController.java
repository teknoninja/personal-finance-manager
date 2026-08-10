package com.financemanager.controller;

import com.financemanager.dto.request.TransactionRequest;
import com.financemanager.dto.request.TransactionUpdateRequest;
import com.financemanager.dto.response.MessageResponse;
import com.financemanager.dto.response.TransactionListResponse;
import com.financemanager.dto.response.TransactionResponse;
import com.financemanager.entity.TransactionType;
import com.financemanager.entity.User;
import com.financemanager.service.CurrentUserProvider;
import com.financemanager.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionRequest request) {
        User user = currentUserProvider.getCurrentUser();
        TransactionResponse response = transactionService.createTransaction(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<TransactionListResponse> getAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) TransactionType type) {
        User user = currentUserProvider.getCurrentUser();
        var transactions = transactionService.getTransactions(user, startDate, endDate, categoryId, type);
        return ResponseEntity.ok(new TransactionListResponse(transactions));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> update(@PathVariable Long id,
                                                        @Valid @RequestBody TransactionUpdateRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(transactionService.updateTransaction(user, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long id) {
        User user = currentUserProvider.getCurrentUser();
        transactionService.deleteTransaction(user, id);
        return ResponseEntity.ok(new MessageResponse("Transaction deleted successfully"));
    }
}
