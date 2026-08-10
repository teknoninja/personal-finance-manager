package com.financemanager.service;

import com.financemanager.dto.request.CategoryRequest;
import com.financemanager.dto.response.CategoryResponse;
import com.financemanager.entity.Category;
import com.financemanager.entity.User;
import com.financemanager.exception.ConflictException;
import com.financemanager.exception.ForbiddenOperationException;
import com.financemanager.exception.ResourceNotFoundException;
import com.financemanager.exception.ValidationException;
import com.financemanager.repository.CategoryRepository;
import com.financemanager.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public List<CategoryResponse> getAllCategoriesForUser(User user) {
        List<Category> categories = new ArrayList<>(categoryRepository.findByOwnerIsNull());
        categories.addAll(categoryRepository.findByOwner(user));
        return categories.stream().map(CategoryResponse::fromEntity).collect(Collectors.toList());
    }

    public CategoryResponse createCustomCategory(User user, CategoryRequest request) {
        String name = request.getName().trim();
        if (name.isEmpty()) {
            throw new ValidationException("Category name must not be blank");
        }

        boolean clashesWithDefault = categoryRepository.findByOwnerIsNullAndNameIgnoreCase(name).isPresent();
        boolean clashesWithOwn = categoryRepository.existsByOwnerAndNameIgnoreCase(user, name);
        if (clashesWithDefault || clashesWithOwn) {
            throw new ConflictException("A category with this name already exists for this user");
        }

        Category category = Category.builder()
                .name(name)
                .type(request.getType())
                .isCustom(true)
                .owner(user)
                .build();
        category = categoryRepository.save(category);
        return CategoryResponse.fromEntity(category);
    }

    public void deleteCustomCategory(User user, String name) {
        Category category = categoryRepository.findByOwnerAndNameIgnoreCase(user, name)
                .orElseGet(() -> {
                    // Distinguish "doesn't exist at all" (404) from "exists but is a default category" (403)
                    boolean isDefault = categoryRepository.findByOwnerIsNullAndNameIgnoreCase(name).isPresent();
                    if (isDefault) {
                        throw new ForbiddenOperationException("Default categories cannot be deleted");
                    }
                    throw new ResourceNotFoundException("Category not found: " + name);
                });

        if (!category.isCustom() || category.getOwner() == null) {
            throw new ForbiddenOperationException("Default categories cannot be deleted");
        }

        if (transactionRepository.existsByCategory(category)) {
            throw new ValidationException("Category is referenced by existing transactions and cannot be deleted");
        }

        categoryRepository.delete(category);
    }

    /** Resolves a category name to an accessible {@link Category} for the given user (default or their own custom). */
    public Category resolveCategoryForUser(User user, String name) {
        return categoryRepository.findByOwnerIsNullAndNameIgnoreCase(name)
                .or(() -> categoryRepository.findByOwnerAndNameIgnoreCase(user, name))
                .orElseThrow(() -> new ValidationException("Invalid or inaccessible category: " + name));
    }
}
