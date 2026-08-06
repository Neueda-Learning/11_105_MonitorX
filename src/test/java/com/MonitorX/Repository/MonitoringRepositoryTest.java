package com.MonitorX.Repository;

import com.MonitorX.models.Customer;
import com.MonitorX.models.FraudAlert;
import com.MonitorX.models.Rule;
import com.MonitorX.models.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.KeyHolder;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MonitoringRepository}. The {@link JdbcTemplate} is fully mocked
 * with Mockito so no real database is required; tests focus on SQL delegation, parameter
 * binding, generated-key handling, and null-safe aggregate fallbacks.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MonitoringRepositoryTest {

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private MonitoringRepository repository;

    private void stubGeneratedKey(int generatedId) {
        when(jdbc.update(any(PreparedStatementCreator.class), any(KeyHolder.class))).thenAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            keyHolder.getKeyList().add(Map.of("id", generatedId));
            return 1;
        });
    }

    @Nested
    @DisplayName("Customer queries")
    class CustomerQueries {

        @Test
        @DisplayName("findAllCustomers delegates to JdbcTemplate and returns mapped list")
        void findAllCustomers_returnsList() {
            Customer customer = new Customer(1, "Alice", "ACC-1", "USA");
            when(jdbc.query(anyString(), ArgumentMatchers_any())).thenReturn(List.of(customer));

            List<Customer> result = repository.findAllCustomers();

            assertThat(result).containsExactly(customer);
            verify(jdbc).query(contains("FROM customers"), ArgumentMatchers_any());
        }

        @Test
        @DisplayName("findCustomer returns empty Optional when no row matches")
        void findCustomer_notFound_returnsEmpty() {
            when(jdbc.query(anyString(), ArgumentMatchers_any(), eq(99))).thenReturn(List.of());

            Optional<Customer> result = repository.findCustomer(99);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("findCustomer returns the matching customer when present")
        void findCustomer_found_returnsCustomer() {
            Customer customer = new Customer(1, "Alice", "ACC-1", "USA");
            when(jdbc.query(anyString(), ArgumentMatchers_any(), eq(1))).thenReturn(List.of(customer));

            Optional<Customer> result = repository.findCustomer(1);

            assertThat(result).contains(customer);
        }

        @Test
        @DisplayName("saveCustomer assigns the generated key returned by the database")
        void saveCustomer_assignsGeneratedId() {
            stubGeneratedKey(42);

            Customer saved = repository.saveCustomer(new Customer(0, "Bob", "ACC-2", "India"));

            assertThat(saved.id()).isEqualTo(42);
            assertThat(saved.name()).isEqualTo("Bob");
        }

        @Test
        @DisplayName("updateCustomer issues an UPDATE with the customer's fields and id")
        void updateCustomer_updatesFields() {
            Customer customer = new Customer(5, "Carol", "ACC-3", "UK");

            Customer result = repository.updateCustomer(customer);

            verify(jdbc).update(anyString(), eq("Carol"), eq("ACC-3"), eq("UK"), eq(5));
            assertThat(result).isEqualTo(customer);
        }

        @Test
        @DisplayName("deleteCustomer issues a DELETE keyed by id")
        void deleteCustomer_deletesById() {
            repository.deleteCustomer(7);

            verify(jdbc).update(contains("DELETE FROM customers"), eq(7));
        }
    }

    @Nested
    @DisplayName("Operator authentication query")
    class OperatorQueries {

        @Test
        @DisplayName("getOperatorPasswordHash returns hash when the operator exists")
        void passwordHash_found() {
            when(jdbc.query(anyString(), ArgumentMatchers_any(), eq("admin"))).thenReturn(List.of("hash123"));

            Optional<String> result = repository.getOperatorPasswordHash("admin");

            assertThat(result).contains("hash123");
        }

        @Test
        @DisplayName("getOperatorPasswordHash returns empty when the operator does not exist")
        void passwordHash_notFound() {
            when(jdbc.query(anyString(), ArgumentMatchers_any(), eq("ghost"))).thenReturn(List.of());

            Optional<String> result = repository.getOperatorPasswordHash("ghost");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Transaction queries")
    class TransactionQueries {

        @Test
        @DisplayName("saveTransaction assigns a generated id and persists each reason row")
        void saveTransaction_assignsIdAndSavesReasons() {
            stubGeneratedKey(101);
            Transaction transaction = new Transaction(0, 1, "Alice", BigDecimal.valueOf(500), "PAYEE-1",
                    "USA", LocalDateTime.of(2025, 1, 1, 10, 0), "desc", "FLAGGED", 30,
                    List.of("Reason A", "Reason B"));

            Transaction saved = repository.saveTransaction(transaction);

            assertThat(saved.id()).isEqualTo(101);
            verify(jdbc).update(contains("INSERT INTO transaction_reasons"), eq(101), eq("Reason A"));
            verify(jdbc).update(contains("INSERT INTO transaction_reasons"), eq(101), eq("Reason B"));
        }

        @Test
        @DisplayName("findAllTransactions joins customers and returns the mapped list")
        void findAllTransactions_returnsList() {
            Transaction transaction = new Transaction(1, 1, "Alice", BigDecimal.TEN, "P", "USA",
                    LocalDateTime.now(), "d", "SUCCESS", 0, List.of());
            when(jdbc.query(anyString(), ArgumentMatchers_any())).thenReturn(List.of(transaction));

            List<Transaction> result = repository.findAllTransactions();

            assertThat(result).containsExactly(transaction);
            verify(jdbc).query(contains("JOIN customers"), ArgumentMatchers_any());
        }

        @Test
        @DisplayName("findTransaction returns empty Optional when not found")
        void findTransaction_notFound() {
            when(jdbc.query(anyString(), ArgumentMatchers_any(), eq(404))).thenReturn(List.of());

            Optional<Transaction> result = repository.findTransaction(404);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("deleteTransaction issues a DELETE keyed by id")
        void deleteTransaction_deletesById() {
            repository.deleteTransaction(9);

            verify(jdbc).update(contains("DELETE FROM transactions"), eq(9));
        }

        @Test
        @DisplayName("countRecentTransactions returns 0 when the aggregate query yields null")
        void countRecentTransactions_nullAggregate_returnsZero() {
            when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(1), any(Timestamp.class)))
                    .thenReturn(null);

            int count = repository.countRecentTransactions(1, LocalDateTime.now());

            assertThat(count).isZero();
        }

        @Test
        @DisplayName("countRecentTransactions passes through the aggregate query result")
        void countRecentTransactions_returnsCount() {
            when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(1), any(Timestamp.class)))
                    .thenReturn(4);

            int count = repository.countRecentTransactions(1, LocalDateTime.now());

            assertThat(count).isEqualTo(4);
        }

        @Test
        @DisplayName("hasPaidPayeeBefore returns false when no prior payment exists")
        void hasPaidPayeeBefore_none_returnsFalse() {
            when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(1), eq("PAYEE-X"), eq(5)))
                    .thenReturn(0);

            boolean result = repository.hasPaidPayeeBefore(1, "PAYEE-X", 5);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("hasPaidPayeeBefore returns true when a prior payment exists")
        void hasPaidPayeeBefore_exists_returnsTrue() {
            when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(1), eq("PAYEE-X"), eq(5)))
                    .thenReturn(3);

            boolean result = repository.hasPaidPayeeBefore(1, "PAYEE-X", 5);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("getDailyTransactionTotal returns ZERO when the SUM aggregate is null")
        void dailyTotal_nullSum_returnsZero() {
            when(jdbc.queryForObject(anyString(), eq(BigDecimal.class), eq(1), any(java.sql.Date.class)))
                    .thenReturn(null);

            BigDecimal total = repository.getDailyTransactionTotal(1, LocalDate.of(2025, 1, 1));

            assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("getDailyTransactionTotal passes through a non-null SUM aggregate")
        void dailyTotal_returnsSum() {
            when(jdbc.queryForObject(anyString(), eq(BigDecimal.class), eq(1), any(java.sql.Date.class)))
                    .thenReturn(BigDecimal.valueOf(2500));

            BigDecimal total = repository.getDailyTransactionTotal(1, LocalDate.of(2025, 1, 1));

            assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(2500));
        }
    }

    @Nested
    @DisplayName("Alert queries")
    class AlertQueries {

        @Test
        @DisplayName("saveAlert assigns a generated id, joins reasons with '|' and records history")
        void saveAlert_persistsJoinedReasonsAndHistory() throws Exception {
            ArgumentCaptor<PreparedStatementCreator> pscCaptor = ArgumentCaptor.forClass(PreparedStatementCreator.class);
            when(jdbc.update(pscCaptor.capture(), any(KeyHolder.class))).thenAnswer(invocation -> {
                KeyHolder keyHolder = invocation.getArgument(1);
                keyHolder.getKeyList().add(Map.of("id", 7));
                return 1;
            });

            FraudAlert alert = new FraudAlert(0, 100, 10, "Alice", "HIGH", "OPEN", 70,
                    List.of("Reason A", "Reason B"), "", LocalDateTime.now(), LocalDateTime.now());

            FraudAlert saved = repository.saveAlert(alert);

            assertThat(saved.id()).isEqualTo(7);

            Connection connection = mock(Connection.class);
            PreparedStatement ps = mock(PreparedStatement.class);
            when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(ps);
            pscCaptor.getValue().createPreparedStatement(connection);
            verify(ps).setString(7, "Reason A|Reason B");
            verify(ps).setInt(2, 10);

            verify(jdbc).update(contains("INSERT INTO alert_history"), eq(7), eq("OPEN"), anyString(), any(Timestamp.class));
        }

        @Test
        @DisplayName("saveAlert stores NULL for the rule id column when no rule triggered it")
        void saveAlert_nullRuleId_setsSqlNull() throws Exception {
            ArgumentCaptor<PreparedStatementCreator> pscCaptor = ArgumentCaptor.forClass(PreparedStatementCreator.class);
            when(jdbc.update(pscCaptor.capture(), any(KeyHolder.class))).thenAnswer(invocation -> {
                KeyHolder keyHolder = invocation.getArgument(1);
                keyHolder.getKeyList().add(Map.of("id", 8));
                return 1;
            });

            FraudAlert alert = new FraudAlert(0, 100, null, "Alice", "MEDIUM", "OPEN", 30,
                    List.of("Built-in check"), "", LocalDateTime.now(), LocalDateTime.now());

            repository.saveAlert(alert);

            Connection connection = mock(Connection.class);
            PreparedStatement ps = mock(PreparedStatement.class);
            when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(ps);
            pscCaptor.getValue().createPreparedStatement(connection);
            verify(ps).setNull(2, java.sql.Types.INTEGER);
        }

        @Test
        @DisplayName("findAllAlerts returns the mapped list ordered by creation date")
        void findAllAlerts_returnsList() {
            FraudAlert alert = new FraudAlert(1, 100, 10, "Alice", "HIGH", "OPEN", 70,
                    List.of("r"), "", LocalDateTime.now(), LocalDateTime.now());
            when(jdbc.query(anyString(), ArgumentMatchers_any())).thenReturn(List.of(alert));

            List<FraudAlert> result = repository.findAllAlerts();

            assertThat(result).containsExactly(alert);
            verify(jdbc).query(contains("ORDER BY created_at DESC"), ArgumentMatchers_any());
        }

        @Test
        @DisplayName("findAlert returns empty Optional when no matching alert")
        void findAlert_notFound() {
            when(jdbc.query(anyString(), ArgumentMatchers_any(), eq(500))).thenReturn(List.of());

            Optional<FraudAlert> result = repository.findAlert(500);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("updateAlert issues an UPDATE with status, notes, timestamp and id")
        void updateAlert_updatesFields() {
            FraudAlert alert = new FraudAlert(1, 100, 10, "Alice", "HIGH", "CLOSED", 70,
                    List.of("r"), "resolved", LocalDateTime.now(), LocalDateTime.now());

            repository.updateAlert(alert);

            verify(jdbc).update(anyString(), eq("CLOSED"), eq("resolved"), any(Timestamp.class), eq(1));
        }

        @Test
        @DisplayName("saveAlertHistory inserts a history row, defaulting null notes to empty string")
        void saveAlertHistory_defaultsNullNotes() {
            repository.saveAlertHistory(1, "ACKNOWLEDGED", null);

            verify(jdbc).update(anyString(), eq(1), eq("ACKNOWLEDGED"), eq(""), any(Timestamp.class));
        }

        @Test
        @DisplayName("findRecentActivity applies the requested LIMIT")
        void findRecentActivity_appliesLimit() {
            when(jdbc.query(anyString(), any(RowMapper.class), eq(50))).thenReturn(List.of());

            repository.findRecentActivity(50);

            verify(jdbc).query(contains("LIMIT ?"), any(RowMapper.class), eq(50));
        }
    }

    @Nested
    @DisplayName("Rule queries")
    class RuleQueries {

        @Test
        @DisplayName("saveRule assigns the generated id")
        void saveRule_assignsGeneratedId() {
            stubGeneratedKey(15);
            Rule rule = new Rule(0, "Big Amount", "AMOUNT_THRESHOLD", "HIGH", "{\"threshold\":1000}", true);

            Rule saved = repository.saveRule(rule);

            assertThat(saved.id()).isEqualTo(15);
        }

        @Test
        @DisplayName("updateRule issues an UPDATE with all rule fields")
        void updateRule_updatesFields() {
            Rule rule = new Rule(3, "Velocity", "VELOCITY", "MEDIUM", "{}", false);

            repository.updateRule(rule);

            verify(jdbc).update(anyString(), eq("Velocity"), eq("VELOCITY"), eq("MEDIUM"), eq("{}"), eq(false), eq(3));
        }

        @Test
        @DisplayName("deleteRule issues a DELETE keyed by id")
        void deleteRule_deletesById() {
            repository.deleteRule(3);

            verify(jdbc).update(contains("DELETE FROM rules"), eq(3));
        }

        @Test
        @DisplayName("findRule returns empty Optional when not found")
        void findRule_notFound() {
            when(jdbc.query(anyString(), ArgumentMatchers_any(), eq(77))).thenReturn(List.of());

            Optional<Rule> result = repository.findRule(77);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Demo / maintenance operations")
    class MaintenanceOperations {

        @Test
        @DisplayName("clearAllForDemo clears dependent tables before customers")
        void clearAllForDemo_clearsAllTables() {
            repository.clearAllForDemo();

            verify(jdbc, atLeastOnce()).update(contains("DELETE FROM alert_history"));
            verify(jdbc, atLeastOnce()).update(contains("DELETE FROM alerts"));
            verify(jdbc, atLeastOnce()).update(contains("DELETE FROM transaction_reasons"));
            verify(jdbc, atLeastOnce()).update(contains("DELETE FROM transactions"));
            verify(jdbc, atLeastOnce()).update(contains("DELETE FROM customers"));
        }
    }

    // Helper to avoid unchecked-generic-array warnings when matching RowMapper<T> for any T.
    @SuppressWarnings("unchecked")
    private static <T> RowMapper<T> ArgumentMatchers_any() {
        return any(RowMapper.class);
    }
}
