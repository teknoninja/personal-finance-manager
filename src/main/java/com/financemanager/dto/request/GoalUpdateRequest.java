package com.financemanager.dto.request;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class GoalUpdateRequest {

    @DecimalMin(value = "0.01", message = "targetAmount must be a positive value")
    private BigDecimal targetAmount;

    private LocalDate targetDate;
}
