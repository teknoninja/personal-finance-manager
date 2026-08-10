package com.financemanager.service;

import com.financemanager.dto.request.CategoryRequest;
import com.financemanager.dto.response.CategoryResponse;
import com.financemanager.entity.Category;
import com.financemanager.entity.CategoryType;
import com.financemanager.entity.User;
import com.financemanager.exception.ConflictException;
import com.financemanager.exception.ForbiddenOperationException;
import com.financemanager.exception.ResourceNotFoundException;
import com.financemanager.exception.ValidationException;
import com.financemanager.repository.CategoryRepository;
import com.financemanager.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private TransactionRepository transactionRepository;

    @InjectMocks
    private CategoryService categoryService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("user@example.com");
    }

    @Test
    void createCustomCategory_succeeds_whenNameIsUnique() {
        CategoryRequest request = new CategoryRequest();
        request.setName("SideBusinessIncome");
        request.setType(CategoryType.INCOME);

        when(categoryRepository.findByOwnerIsNullAndNameIgnoreCase("SideBusinessIncome")).thenReturn(Optional.empty());
        when(categoryRepository.existsByOwnerAndNameIgnoreCase(user, "SideBusinessIncome")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        CategoryResponse response = categoryService.createCustomCategory(user, request);

        assertEquals("SideBusinessIncome", response.getName());
        assertTrue(response.isCustom());
    }

    @Test
    void createCustomCategory_throwsConflict_whenNameAlreadyExistsForUser() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Food");
        request.setType(CategoryType.EXPENSE);

        when(categoryRepository.findByOwnerIsNullAndNameIgnoreCase("Food"))
                .thenReturn(Optional.of(Category.builder().name("Food").type(CategoryType.EXPENSE).isCustom(false).build()));

        assertThrows(ConflictException.class, () -> categoryService.createCustomCategory(user, request));
    }

    @Test
    void deleteCustomCategory_throwsForbidden_whenCategoryIsDefault() {
        when(categoryRepository.findByOwnerAndNameIgnoreCase(user, "Food")).thenReturn(Optional.empty());
        when(categoryRepository.findByOwnerIsNullAndNameIgnoreCase("Food"))
                .thenReturn(Optional.of(Category.builder().name("Food").type(CategoryType.EXPENSE).isCustom(false).build()));

        assertThrows(ForbiddenOperationException.class, () -> categoryService.deleteCustomCategory(user, "Food"));
    }

    @Test
    void deleteCustomCategory_throwsNotFound_whenCategoryDoesNotExist() {
        when(categoryRepository.findByOwnerAndNameIgnoreCase(user, "Ghost")).thenReturn(Optional.empty());
        when(categoryRepository.findByOwnerIsNullAndNameIgnoreCase("Ghost")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.deleteCustomCategory(user, "Ghost"));
    }

    @Test
    void deleteCustomCategory_throwsValidation_whenReferencedByTransactions() {
        Category custom = Category.builder().id(5L).name("Hobby").type(CategoryType.EXPENSE).isCustom(true).owner(user).build();
        when(categoryRepository.findByOwnerAndNameIgnoreCase(user, "Hobby")).thenReturn(Optional.of(custom));
        when(transactionRepository.existsByCategory(custom)).thenReturn(true);

        assertThrows(ValidationException.class, () -> categoryService.deleteCustomCategory(user, "Hobby"));
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void resolveCategoryForUser_throwsValidation_whenCategoryInaccessible() {
        when(categoryRepository.findByOwnerIsNullAndNameIgnoreCase("Unknown")).thenReturn(Optional.empty());
        when(categoryRepository.findByOwnerAndNameIgnoreCase(user, "Unknown")).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> categoryService.resolveCategoryForUser(user, "Unknown"));
    }
}
