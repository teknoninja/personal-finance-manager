package com.financemanager.service;

import com.financemanager.dto.request.GoalRequest;
import com.financemanager.dto.request.GoalUpdateRequest;
import com.financemanager.dto.response.GoalResponse;
import com.financemanager.entity.SavingsGoal;
import com.financemanager.entity.TransactionType;
import com.financemanager.entity.User;
import com.financemanager.exception.ForbiddenOperationException;
import com.financemanager.exception.ResourceNotFoundException;
import com.financemanager.exception.ValidationException;
import com.financemanager.repository.SavingsGoalRepository;
import com.financemanager.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final TransactionRepository transactionRepository;

    public GoalResponse createGoal(User user, GoalRequest request) {
        if (!request.getTargetDate().isAfter(LocalDate.now())) {
            throw new ValidationException("targetDate must be a future date");
        }

        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();

        SavingsGoal goal = SavingsGoal.builder()
                .goalName(request.getGoalName())
                .targetAmount(request.getTargetAmount())
                .targetDate(request.getTargetDate())
                .startDate(startDate)
                .user(user)
                .build();

        goal = savingsGoalRepository.save(goal);
        return toResponse(goal);
    }

    public List<GoalResponse> getAllGoals(User user) {
        return savingsGoalRepository.findByUser(user).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public GoalResponse getGoal(User user, Long id) {
        return toResponse(getOwnedGoal(user, id));
    }

    public GoalResponse updateGoal(User user, Long id, GoalUpdateRequest request) {
        SavingsGoal goal = getOwnedGoal(user, id);

        if (request.getTargetAmount() != null) {
            goal.setTargetAmount(request.getTargetAmount());
        }
        if (request.getTargetDate() != null) {
            if (!request.getTargetDate().isAfter(LocalDate.now())) {
                throw new ValidationException("targetDate must be a future date");
            }
            goal.setTargetDate(request.getTargetDate());
        }

        goal = savingsGoalRepository.save(goal);
        return toResponse(goal);
    }

    public void deleteGoal(User user, Long id) {
        SavingsGoal goal = getOwnedGoal(user, id);
        savingsGoalRepository.delete(goal);
    }

    private SavingsGoal getOwnedGoal(User user, Long id) {
        SavingsGoal goal = savingsGoalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found: " + id));
        if (!goal.getUser().getId().equals(user.getId())) {
            throw new ForbiddenOperationException("You do not have access to this savings goal");
        }
        return goal;
    }

    private GoalResponse toResponse(SavingsGoal goal) {
        BigDecimal income = transactionRepository.sumByUserAndTypeSince(goal.getUser(), TransactionType.INCOME, goal.getStartDate());
        BigDecimal expenses = transactionRepository.sumByUserAndTypeSince(goal.getUser(), TransactionType.EXPENSE, goal.getStartDate());
        BigDecimal progress = income.subtract(expenses);

        double percentage = goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0
                ? progress.divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue()
                : 0.0;
        // Clamp for a sane display value while keeping the raw progress/remaining figures exact.
        if (percentage < 0) percentage = 0.0;

        BigDecimal remaining = goal.getTargetAmount().subtract(progress);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }

        return new GoalResponse(
                goal.getId(),
                goal.getGoalName(),
                goal.getTargetAmount(),
                goal.getTargetDate(),
                goal.getStartDate(),
                progress,
                percentage,
                remaining
        );
    }
}
