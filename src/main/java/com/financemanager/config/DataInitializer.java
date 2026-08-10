package com.financemanager.config;

import com.financemanager.entity.Category;
import com.financemanager.entity.CategoryType;
import com.financemanager.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/** Seeds the system-wide default categories on application startup, if not already present. */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    private static final List<String> DEFAULT_INCOME = List.of("Salary");
    private static final List<String> DEFAULT_EXPENSE = List.of(
            "Food", "Rent", "Transportation", "Entertainment", "Healthcare", "Utilities"
    );

    @Override
    public void run(String... args) {
        for (String name : DEFAULT_INCOME) {
            seedIfMissing(name, CategoryType.INCOME);
        }
        for (String name : DEFAULT_EXPENSE) {
            seedIfMissing(name, CategoryType.EXPENSE);
        }
    }

    private void seedIfMissing(String name, CategoryType type) {
        categoryRepository.findByOwnerIsNullAndNameIgnoreCase(name).ifPresentOrElse(
                existing -> { /* already seeded */ },
                () -> categoryRepository.save(
                        Category.builder().name(name).type(type).isCustom(false).owner(null).build()
                )
        );
    }
}
