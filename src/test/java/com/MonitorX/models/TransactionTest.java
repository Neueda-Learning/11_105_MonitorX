package com.MonitorX.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic accessor/equality tests for the {@link Transaction} record.
 */
class TransactionTest {

    @Test
    @DisplayName("Accessors expose the constructor values, including reasons list")
    void accessors_exposeConstructorValues() {
        LocalDateTime timestamp = LocalDateTime.of(2025, 1, 1, 12, 0);
        Transaction transaction = new Transaction(1, 10, "Alice", BigDecimal.valueOf(500), "PAYEE-1",
                "USA", timestamp, "desc", "FLAGGED", 30, List.of("Reason A"));

        assertThat(transaction.id()).isEqualTo(1);
        assertThat(transaction.customerId()).isEqualTo(10);
        assertThat(transaction.customerName()).isEqualTo("Alice");
        assertThat(transaction.amount()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(transaction.payeeId()).isEqualTo("PAYEE-1");
        assertThat(transaction.transactionCountry()).isEqualTo("USA");
        assertThat(transaction.timestamp()).isEqualTo(timestamp);
        assertThat(transaction.description()).isEqualTo("desc");
        assertThat(transaction.status()).isEqualTo("FLAGGED");
        assertThat(transaction.riskScore()).isEqualTo(30);
        assertThat(transaction.reasons()).containsExactly("Reason A");
    }

    @Test
    @DisplayName("A transaction with no reasons has an empty reasons list, not null")
    void noReasons_hasEmptyList() {
        Transaction transaction = new Transaction(1, 10, "Alice", BigDecimal.TEN, "P", "USA",
                LocalDateTime.now(), "d", "SUCCESS", 0, List.of());

        assertThat(transaction.reasons()).isEmpty();
    }

    @Test
    @DisplayName("Transactions with identical field values are equal")
    void equalTransactions_areEqual() {
        LocalDateTime timestamp = LocalDateTime.of(2025, 1, 1, 12, 0);
        Transaction a = new Transaction(1, 10, "Alice", BigDecimal.TEN, "P", "USA", timestamp, "d", "SUCCESS", 0, List.of());
        Transaction b = new Transaction(1, 10, "Alice", BigDecimal.TEN, "P", "USA", timestamp, "d", "SUCCESS", 0, List.of());

        assertThat(a).isEqualTo(b);
    }
}
