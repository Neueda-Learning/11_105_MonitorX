package com.MonitorX.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record Transaction(
        int id,
        int customerId,
        String customerName,
        BigDecimal amount,
        String transactionCountry,
        LocalDateTime timestamp,
        String description,
        String status,
        int riskScore,
        List<String> reasons
) {}
