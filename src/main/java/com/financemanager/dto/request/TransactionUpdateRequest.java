package com.financemanager.dto.request;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

/** Date is intentionally excluded - the date field cannot be updated per spec. */
@Data
public class TransactionUpdateRequest {

    @DecimalMin(value = "0.01", message = "amount must be a positive value")
    private BigDecimal amount;

    private String category;

    private String description;
}
