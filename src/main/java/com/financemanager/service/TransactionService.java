package com.financemanager.service;

import com.financemanager.dto.request.TransactionRequest;
import com.financemanager.dto.request.TransactionUpdateRequest;
import com.financemanager.dto.response.TransactionResponse;
import com.financemanager.entity.Category;
import com.financemanager.entity.Transaction;
import com.financemanager.entity.TransactionType;
import com.financemanager.entity.User;
import com.financemanager.exception.ForbiddenOperationException;
import com.financemanager.exception.ResourceNotFoundException;
import com.financemanager.exception.ValidationException;
import com.financemanager.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;

    public TransactionResponse createTransaction(User user, TransactionRequest request) {
        if (request.getDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Transaction date cannot be in the future");
        }

        Category category = categoryService.resolveCategoryForUser(user, request.getCategory());

        Transaction transaction = Transaction.builder()
                .amount(request.getAmount())
                .date(request.getDate())
                .category(category)
                .description(request.getDescription())
                .type(category.getType() == com.financemanager.entity.CategoryType.INCOME
                        ? TransactionType.INCOME : TransactionType.EXPENSE)
                .user(user)
                .build();

        transaction = transactionRepository.save(transaction);
        return TransactionResponse.fromEntity(transaction);
    }

    public List<TransactionResponse> getTransactions(User user, LocalDate startDate, LocalDate endDate,
                                                       Long categoryId, TransactionType type) {
        return transactionRepository.findFiltered(user, startDate, endDate, categoryId, type)
                .stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public TransactionResponse updateTransaction(User user, Long id, TransactionUpdateRequest request) {
        Transaction transaction = getOwnedTransaction(user, id);

        if (request.getAmount() != null) {
            transaction.setAmount(request.getAmount());
        }
        if (request.getDescription() != null) {
            transaction.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            Category category = categoryService.resolveCategoryForUser(user, request.getCategory());
            transaction.setCategory(category);
            transaction.setType(category.getType() == com.financemanager.entity.CategoryType.INCOME
                    ? TransactionType.INCOME : TransactionType.EXPENSE);
        }

        transaction = transactionRepository.save(transaction);
        return TransactionResponse.fromEntity(transaction);
    }

    public void deleteTransaction(User user, Long id) {
        Transaction transaction = getOwnedTransaction(user, id);
        transactionRepository.delete(transaction);
    }

    private Transaction getOwnedTransaction(User user, Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new ForbiddenOperationException("You do not have access to this transaction");
        }
        return transaction;
    }
}
