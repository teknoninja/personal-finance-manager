package com.financemanager.dto.request;

import com.financemanager.entity.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotNull(message = "type is required and must be INCOME or EXPENSE")
    private CategoryType type;
}
