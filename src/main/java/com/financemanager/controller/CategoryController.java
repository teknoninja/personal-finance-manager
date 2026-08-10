package com.financemanager.controller;

import com.financemanager.dto.request.CategoryRequest;
import com.financemanager.dto.response.CategoryListResponse;
import com.financemanager.dto.response.CategoryResponse;
import com.financemanager.dto.response.MessageResponse;
import com.financemanager.entity.User;
import com.financemanager.service.CategoryService;
import com.financemanager.service.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public ResponseEntity<CategoryListResponse> getAll() {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(new CategoryListResponse(categoryService.getAllCategoriesForUser(user)));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        User user = currentUserProvider.getCurrentUser();
        CategoryResponse response = categoryService.createCustomCategory(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<MessageResponse> delete(@PathVariable String name) {
        User user = currentUserProvider.getCurrentUser();
        categoryService.deleteCustomCategory(user, name);
        return ResponseEntity.ok(new MessageResponse("Category deleted successfully"));
    }
}
