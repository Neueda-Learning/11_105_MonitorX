package com.MonitorX.models;

import java.math.BigDecimal;

public record DashboardSummary(
        int totalTransactions,
        int flaggedTransactions,
        int openAlerts,
        BigDecimal totalVolume
) {}
