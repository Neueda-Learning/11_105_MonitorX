package com.MonitorX.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic accessor/equality tests for the {@link DashboardSummary} record.
 */
class DashboardSummaryTest {

    @Test
    @DisplayName("Accessors expose the constructor values")
    void accessors_exposeConstructorValues() {
        DashboardSummary summary = new DashboardSummary(100, 5, 2, BigDecimal.valueOf(75000));

        assertThat(summary.totalTransactions()).isEqualTo(100);
        assertThat(summary.flaggedTransactions()).isEqualTo(5);
        assertThat(summary.openAlerts()).isEqualTo(2);
        assertThat(summary.totalVolume()).isEqualByComparingTo(BigDecimal.valueOf(75000));
    }

    @Test
    @DisplayName("Summaries with identical field values are equal")
    void equalSummaries_areEqual() {
        DashboardSummary a = new DashboardSummary(100, 5, 2, BigDecimal.valueOf(75000));
        DashboardSummary b = new DashboardSummary(100, 5, 2, BigDecimal.valueOf(75000));

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
