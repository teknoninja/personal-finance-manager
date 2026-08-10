package com.financemanager.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionRequest {

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be a positive value")
    private BigDecimal amount;

    @NotNull(message = "date is required")
    private LocalDate date;

    @NotBlank(message = "category is required")
    private String category;

    private String description;
}
