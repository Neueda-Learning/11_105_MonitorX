package com.MonitorX.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic accessor/equality tests for the {@link AlertHistoryItem} record.
 */
class AlertHistoryItemTest {

    @Test
    @DisplayName("Accessors expose the constructor values")
    void accessors_exposeConstructorValues() {
        LocalDateTime changedAt = LocalDateTime.of(2025, 1, 1, 9, 0);
        AlertHistoryItem item = new AlertHistoryItem(1, 5, "ACKNOWLEDGED", "Reviewing", changedAt);

        assertThat(item.id()).isEqualTo(1);
        assertThat(item.alertId()).isEqualTo(5);
        assertThat(item.status()).isEqualTo("ACKNOWLEDGED");
        assertThat(item.operatorNotes()).isEqualTo("Reviewing");
        assertThat(item.changedAt()).isEqualTo(changedAt);
    }

    @Test
    @DisplayName("Items with identical field values are equal")
    void equalItems_areEqual() {
        LocalDateTime changedAt = LocalDateTime.of(2025, 1, 1, 9, 0);
        AlertHistoryItem a = new AlertHistoryItem(1, 5, "OPEN", "created", changedAt);
        AlertHistoryItem b = new AlertHistoryItem(1, 5, "OPEN", "created", changedAt);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
