package com.MonitorX.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic accessor/equality tests for the {@link AuditLogEntry} record.
 */
class AuditLogEntryTest {

    @Test
    @DisplayName("Accessors expose the constructor values")
    void accessors_exposeConstructorValues() {
        LocalDateTime changedAt = LocalDateTime.of(2025, 1, 1, 9, 0);
        AuditLogEntry entry = new AuditLogEntry(1, 5, 100, "Alice", "HIGH", "CLOSED", "resolved", changedAt);

        assertThat(entry.historyId()).isEqualTo(1);
        assertThat(entry.alertId()).isEqualTo(5);
        assertThat(entry.transactionId()).isEqualTo(100);
        assertThat(entry.customerName()).isEqualTo("Alice");
        assertThat(entry.severity()).isEqualTo("HIGH");
        assertThat(entry.status()).isEqualTo("CLOSED");
        assertThat(entry.operatorNotes()).isEqualTo("resolved");
        assertThat(entry.changedAt()).isEqualTo(changedAt);
    }

    @Test
    @DisplayName("Entries with identical field values are equal")
    void equalEntries_areEqual() {
        LocalDateTime changedAt = LocalDateTime.of(2025, 1, 1, 9, 0);
        AuditLogEntry a = new AuditLogEntry(1, 5, 100, "Alice", "HIGH", "CLOSED", "resolved", changedAt);
        AuditLogEntry b = new AuditLogEntry(1, 5, 100, "Alice", "HIGH", "CLOSED", "resolved", changedAt);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
