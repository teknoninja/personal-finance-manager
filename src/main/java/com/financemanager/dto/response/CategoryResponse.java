package com.financemanager.dto.response;

import com.financemanager.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private String name;
    private String type;
    private boolean isCustom;

    public static CategoryResponse fromEntity(Category c) {
        return new CategoryResponse(c.getName(), c.getType().name(), c.isCustom());
    }
}
