package com.MonitorX.models;

import java.time.LocalDateTime;

public record AlertHistoryItem(
        int id,
        int alertId,
        String status,
        String operatorNotes,
        LocalDateTime changedAt
) {}
