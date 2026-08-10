package com.financemanager.service;

import com.financemanager.dto.request.GoalRequest;
import com.financemanager.dto.request.GoalUpdateRequest;
import com.financemanager.dto.response.GoalResponse;
import com.financemanager.entity.SavingsGoal;
import com.financemanager.entity.TransactionType;
import com.financemanager.entity.User;
import com.financemanager.exception.ForbiddenOperationException;
import com.financemanager.exception.ValidationException;
import com.financemanager.repository.SavingsGoalRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock private SavingsGoalRepository savingsGoalRepository;
    @Mock private TransactionRepository transactionRepository;

    @InjectMocks
    private GoalService goalService;

    private User user;
    private User otherUser;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        otherUser = new User();
        otherUser.setId(2L);
    }

    @Test
    void createGoal_throwsValidation_whenTargetDateNotInFuture() {
        GoalRequest request = new GoalRequest();
        request.setGoalName("Emergency Fund");
        request.setTargetAmount(BigDecimal.valueOf(5000));
        request.setTargetDate(LocalDate.now());

        assertThrows(ValidationException.class, () -> goalService.createGoal(user, request));
    }

    @Test
    void createGoal_computesProgress_fromIncomeMinusExpenses() {
        GoalRequest request = new GoalRequest();
        request.setGoalName("Emergency Fund");
        request.setTargetAmount(BigDecimal.valueOf(5000));
        request.setTargetDate(LocalDate.now().plusMonths(6));
        request.setStartDate(LocalDate.now().minusMonths(1));

        when(savingsGoalRepository.save(any(SavingsGoal.class))).thenAnswer(inv -> {
            SavingsGoal g = inv.getArgument(0);
            g.setId(1L);
            return g;
        });
        when(transactionRepository.sumByUserAndTypeSince(eq(user), eq(TransactionType.INCOME), any()))
                .thenReturn(BigDecimal.valueOf(3000));
        when(transactionRepository.sumByUserAndTypeSince(eq(user), eq(TransactionType.EXPENSE), any()))
                .thenReturn(BigDecimal.valueOf(2000));

        GoalResponse response = goalService.createGoal(user, request);

        assertEquals(BigDecimal.valueOf(1000), response.getCurrentProgress());
        assertEquals(20.0, response.getProgressPercentage());
        assertEquals(BigDecimal.valueOf(4000), response.getRemainingAmount());
    }

    @Test
    void getGoal_throwsForbidden_whenGoalOwnedByAnotherUser() {
        SavingsGoal goal = SavingsGoal.builder().id(1L).user(otherUser).goalName("x")
                .targetAmount(BigDecimal.TEN).targetDate(LocalDate.now().plusDays(30)).startDate(LocalDate.now()).build();
        when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(goal));

        assertThrows(ForbiddenOperationException.class, () -> goalService.getGoal(user, 1L));
    }

    @Test
    void updateGoal_throwsValidation_whenNewTargetDateNotFuture() {
        SavingsGoal goal = SavingsGoal.builder().id(1L).user(user).goalName("x")
                .targetAmount(BigDecimal.TEN).targetDate(LocalDate.now().plusDays(30)).startDate(LocalDate.now()).build();
        when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(goal));

        GoalUpdateRequest update = new GoalUpdateRequest();
        update.setTargetDate(LocalDate.now().minusDays(1));

        assertThrows(ValidationException.class, () -> goalService.updateGoal(user, 1L, update));
    }
}
