package com.MonitorX.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TransactionRequest(
        @NotNull
        @Positive
        Integer customerId,
        @NotNull
        @DecimalMin("0.01")
        BigDecimal amount,
        @NotBlank
        @Size(max = 60)
        String payeeId,
        @NotBlank
        @Size(max = 60)
        String transactionCountry,
        LocalDateTime timestamp,
        @Size(max = 160)
        String description
        ) {

}
