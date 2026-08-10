package com.financemanager.service;

import com.financemanager.dto.request.TransactionRequest;
import com.financemanager.dto.request.TransactionUpdateRequest;
import com.financemanager.dto.response.TransactionResponse;
import com.financemanager.entity.*;
import com.financemanager.exception.ForbiddenOperationException;
import com.financemanager.exception.ResourceNotFoundException;
import com.financemanager.exception.ValidationException;
import com.financemanager.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private CategoryService categoryService;

    @InjectMocks
    private TransactionService transactionService;

    private User user;
    private User otherUser;
    private Category salaryCategory;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        otherUser = new User();
        otherUser.setId(2L);
        salaryCategory = Category.builder().id(1L).name("Salary").type(CategoryType.INCOME).isCustom(false).build();
    }

    @Test
    void createTransaction_throwsValidation_whenDateInFuture() {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(BigDecimal.TEN);
        request.setDate(LocalDate.now().plusDays(1));
        request.setCategory("Salary");

        assertThrows(ValidationException.class, () -> transactionService.createTransaction(user, request));
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void createTransaction_succeeds_andDerivesTypeFromCategory() {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(BigDecimal.valueOf(5000));
        request.setDate(LocalDate.now());
        request.setCategory("Salary");
        request.setDescription("Monthly pay");

        when(categoryService.resolveCategoryForUser(user, "Salary")).thenReturn(salaryCategory);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(100L);
            return t;
        });

        TransactionResponse response = transactionService.createTransaction(user, request);

        assertEquals("INCOME", response.getType());
        assertEquals("Salary", response.getCategory());
    }

    @Test
    void updateTransaction_throwsForbidden_whenNotOwnedByUser() {
        Transaction existing = Transaction.builder().id(1L).user(otherUser).category(salaryCategory)
                .amount(BigDecimal.TEN).date(LocalDate.now()).type(TransactionType.INCOME).build();
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(existing));

        TransactionUpdateRequest updateRequest = new TransactionUpdateRequest();
        updateRequest.setAmount(BigDecimal.valueOf(20));

        assertThrows(ForbiddenOperationException.class, () -> transactionService.updateTransaction(user, 1L, updateRequest));
    }

    @Test
    void deleteTransaction_throwsNotFound_whenTransactionMissing() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> transactionService.deleteTransaction(user, 99L));
    }

    @Test
    void deleteTransaction_deletes_whenOwnedByUser() {
        Transaction existing = Transaction.builder().id(1L).user(user).category(salaryCategory)
                .amount(BigDecimal.TEN).date(LocalDate.now()).type(TransactionType.INCOME).build();
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(existing));

        transactionService.deleteTransaction(user, 1L);

        verify(transactionRepository).delete(existing);
    }
}
