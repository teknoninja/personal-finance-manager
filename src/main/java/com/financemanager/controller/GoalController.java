package com.financemanager.controller;

import com.financemanager.dto.request.GoalRequest;
import com.financemanager.dto.request.GoalUpdateRequest;
import com.financemanager.dto.response.GoalListResponse;
import com.financemanager.dto.response.GoalResponse;
import com.financemanager.dto.response.MessageResponse;
import com.financemanager.entity.User;
import com.financemanager.service.CurrentUserProvider;
import com.financemanager.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    public ResponseEntity<GoalResponse> create(@Valid @RequestBody GoalRequest request) {
        User user = currentUserProvider.getCurrentUser();
        GoalResponse response = goalService.createGoal(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<GoalListResponse> getAll() {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(new GoalListResponse(goalService.getAllGoals(user)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoalResponse> get(@PathVariable Long id) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(goalService.getGoal(user, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalResponse> update(@PathVariable Long id, @Valid @RequestBody GoalUpdateRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(goalService.updateGoal(user, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long id) {
        User user = currentUserProvider.getCurrentUser();
        goalService.deleteGoal(user, id);
        return ResponseEntity.ok(new MessageResponse("Goal deleted successfully"));
    }
}
