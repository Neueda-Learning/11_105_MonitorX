package com.MonitorX.models;

import java.time.LocalDateTime;

public record AuditLogEntry(
        int historyId,
        int alertId,
        int transactionId,
        String customerName,
        String severity,
        String status,
        String operatorNotes,
        LocalDateTime changedAt
) {}
