package com.MonitorX.models;

import java.time.LocalDateTime;
import java.util.List;

public record FraudAlert(
        int id,
        int transactionId,
        String customerName,
        String severity,
        String status,
        int riskScore,
        List<String> reasons,
        LocalDateTime createdAt
) {
    public FraudAlert withStatus(String nextStatus) {
        return new FraudAlert(id, transactionId, customerName, severity, nextStatus,
                riskScore, reasons, createdAt);
    }
}
