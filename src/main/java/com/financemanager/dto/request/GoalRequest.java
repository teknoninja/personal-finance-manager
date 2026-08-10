package com.financemanager.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class GoalRequest {

    @NotBlank(message = "goalName is required")
    private String goalName;

    @NotNull(message = "targetAmount is required")
    @DecimalMin(value = "0.01", message = "targetAmount must be a positive value")
    private BigDecimal targetAmount;

    @NotNull(message = "targetDate is required")
    private LocalDate targetDate;

    /** Optional - defaults to today (creation date) if not supplied. */
    private LocalDate startDate;
}
