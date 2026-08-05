package com.MonitorX.models;

import java.time.LocalDateTime;
import java.util.List;

public record FraudAlert(
        int id,
        int transactionId,
        Integer ruleId,
        String customerName,
        String severity,
        String status,
        int riskScore,
        List<String> reasons,
        String resolutionNotes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public FraudAlert withStatus(String nextStatus, String notes) {
        return new FraudAlert(id, transactionId, ruleId, customerName, severity, nextStatus,
                riskScore, reasons, notes, createdAt, LocalDateTime.now());
    }
}
