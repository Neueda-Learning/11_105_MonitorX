package com.MonitorX.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FraudAlert}, focused on the {@code withStatus} lifecycle-transition
 * helper used by {@code FraudDetectionService.updateAlertStatus}.
 */
class FraudAlertTest {

    @Test
    @DisplayName("withStatus updates status and notes, refreshes updatedAt, and preserves all other fields")
    void withStatus_updatesStatusNotesAndTimestamp_preservesOtherFields() {
        LocalDateTime created = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime originalUpdated = LocalDateTime.of(2025, 1, 1, 10, 0);
        FraudAlert original = new FraudAlert(1, 100, 10, "Alice", "HIGH", "OPEN", 70,
                List.of("Reason A"), "", created, originalUpdated);

        FraudAlert updated = original.withStatus("ACKNOWLEDGED", "Reviewing now");

        assertThat(updated.status()).isEqualTo("ACKNOWLEDGED");
        assertThat(updated.resolutionNotes()).isEqualTo("Reviewing now");
        assertThat(updated.updatedAt()).isAfterOrEqualTo(originalUpdated);
        assertThat(updated.id()).isEqualTo(original.id());
        assertThat(updated.transactionId()).isEqualTo(original.transactionId());
        assertThat(updated.ruleId()).isEqualTo(original.ruleId());
        assertThat(updated.customerName()).isEqualTo(original.customerName());
        assertThat(updated.severity()).isEqualTo(original.severity());
        assertThat(updated.riskScore()).isEqualTo(original.riskScore());
        assertThat(updated.reasons()).isEqualTo(original.reasons());
        assertThat(updated.createdAt()).isEqualTo(original.createdAt());
    }

    @Test
    @DisplayName("withStatus does not mutate the original instance (records are immutable)")
    void withStatus_doesNotMutateOriginal() {
        FraudAlert original = new FraudAlert(1, 100, null, "Alice", "MEDIUM", "OPEN", 30,
                List.of("Reason"), "", LocalDateTime.now(), LocalDateTime.now());

        original.withStatus("DISMISSED", "Not fraud");

        assertThat(original.status()).isEqualTo("OPEN");
        assertThat(original.resolutionNotes()).isEqualTo("");
    }

    @Test
    @DisplayName("Alerts with identical field values are equal")
    void equalAlerts_areEqual() {
        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 0, 0);
        FraudAlert a = new FraudAlert(1, 100, 10, "Alice", "HIGH", "OPEN", 70, List.of("r"), "", now, now);
        FraudAlert b = new FraudAlert(1, 100, 10, "Alice", "HIGH", "OPEN", 70, List.of("r"), "", now, now);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("ruleId can be null for alerts raised purely by built-in checks")
    void nullRuleId_isAllowed() {
        FraudAlert alert = new FraudAlert(1, 100, null, "Alice", "LOW", "OPEN", 20,
                List.of("Transaction made during unusual hours"), "", LocalDateTime.now(), LocalDateTime.now());

        assertThat(alert.ruleId()).isNull();
    }
}
