package com.MonitorX.Services;

import com.MonitorX.Repository.MonitoringRepository;
import com.MonitorX.models.AlertHistoryItem;
import com.MonitorX.models.AuditLogEntry;
import com.MonitorX.models.Customer;
import com.MonitorX.models.DashboardSummary;
import com.MonitorX.models.FraudAlert;
import com.MonitorX.models.Rule;
import com.MonitorX.models.Transaction;
import com.MonitorX.models.TransactionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link FraudDetectionService} covering the core fraud-detection
 * business scenarios: location mismatch, unusual hours, dynamic rule evaluation
 * (amount threshold, velocity, new payee, daily limit), combined risk scoring,
 * alert creation/severity and the transaction/alert lifecycle.
 *
 * The {@link MonitoringRepository} is fully mocked with Mockito; a real
 * {@link ObjectMapper} is used since the service relies on it to parse rule
 * parameter JSON.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FraudDetectionServiceTest {

    @Mock
    private MonitoringRepository repository;

    private ObjectMapper objectMapper;
    private FraudDetectionService service;

    private Customer customer;
    private final AtomicInteger txIdSequence = new AtomicInteger(0);

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new FraudDetectionService(repository, objectMapper);

        customer = new Customer(1, "Alice Doe", "ACC-001", "USA");
        when(repository.findCustomer(1)).thenReturn(Optional.of(customer));

        // No dynamic rules unless a test overrides this.
        when(repository.findAllRules()).thenReturn(List.of());

        // Echo back saved transactions with an auto-incrementing id, mimicking DB behavior.
        when(repository.saveTransaction(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            int id = txIdSequence.incrementAndGet();
            return new Transaction(id, t.customerId(), t.customerName(), t.amount(), t.payeeId(),
                    t.transactionCountry(), t.timestamp(), t.description(), t.status(), t.riskScore(), t.reasons());
        });

        when(repository.saveAlert(any(FraudAlert.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private TransactionRequest request(BigDecimal amount, String country, LocalDateTime timestamp, String payeeId) {
        return new TransactionRequest(1, amount, payeeId, country, timestamp, "Test transaction");
    }

    private Rule rule(int id, String type, String parametersJson, boolean active) {
        return new Rule(id, type + "-rule", type, "MEDIUM", parametersJson, active);
    }

    // ---------------------------------------------------------------------
    // Baseline / clean transaction scenarios
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("Clean transactions")
    class CleanTransactions {

        @Test
        @DisplayName("Same country, normal business hours, no rules -> SUCCESS with zero risk and no alert")
        void legitimateTransaction_isNotFlagged() {
            TransactionRequest req = request(BigDecimal.valueOf(100),
                    "USA", LocalDateTime.of(2025, 1, 1, 14, 0), "PAYEE-1");

            Transaction result = service.processTransaction(req);

            assertThat(result.status()).isEqualTo("SUCCESS");
            assertThat(result.riskScore()).isZero();
            assertThat(result.reasons()).isEmpty();
            verify(repository, never()).saveAlert(any());
        }

        @Test
        @DisplayName("Null timestamp defaults to current time")
        void nullTimestamp_defaultsToNow() {
            TransactionRequest req = new TransactionRequest(1, BigDecimal.TEN, "PAYEE-1", "USA", null, "desc");

            Transaction result = service.processTransaction(req);

            assertThat(result).isNotNull();
            // Must not throw and must persist a transaction with some timestamp assigned by the service.
            verify(repository, times(2)).saveTransaction(any(Transaction.class));
        }

        @Test
        @DisplayName("Null description is normalized to empty string")
        void nullDescription_isNormalized() {
            TransactionRequest req = new TransactionRequest(1, BigDecimal.TEN, "PAYEE-1", "USA",
                    LocalDateTime.of(2025, 1, 1, 12, 0), null);

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            service.processTransaction(req);

            verify(repository, times(2)).saveTransaction(captor.capture());
            assertThat(captor.getAllValues().get(1).description()).isEqualTo("");
        }
    }

    // ---------------------------------------------------------------------
    // Built-in checks: location mismatch & unusual hours
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("Built-in fraud checks")
    class BuiltInChecks {

        @Test
        @DisplayName("Transaction country differs from customer's registered country -> flagged with +30 risk")
        void countryMismatch_isFlagged() {
            TransactionRequest req = request(BigDecimal.valueOf(100),
                    "United Kingdom", LocalDateTime.of(2025, 1, 1, 14, 0), "PAYEE-1");

            Transaction result = service.processTransaction(req);

            assertThat(result.status()).isEqualTo("FLAGGED");
            assertThat(result.riskScore()).isEqualTo(30);
            assertThat(result.reasons()).containsExactly("Transaction country differs from customer country");
            verify(repository).saveAlert(any(FraudAlert.class));
        }

        @Test
        @DisplayName("Country check is case-insensitive")
        void countryCheck_isCaseInsensitive() {
            TransactionRequest req = request(BigDecimal.valueOf(100),
                    "usa", LocalDateTime.of(2025, 1, 1, 14, 0), "PAYEE-1");

            Transaction result = service.processTransaction(req);

            assertThat(result.status()).isEqualTo("SUCCESS");
            assertThat(result.riskScore()).isZero();
        }

        @Test
        @DisplayName("Transaction made before 5 AM -> flagged with +20 risk for unusual hours")
        void unusualHours_isFlagged() {
            TransactionRequest req = request(BigDecimal.valueOf(100),
                    "USA", LocalDateTime.of(2025, 1, 1, 3, 30), "PAYEE-1");

            Transaction result = service.processTransaction(req);

            assertThat(result.status()).isEqualTo("FLAGGED");
            assertThat(result.riskScore()).isEqualTo(20);
            assertThat(result.reasons()).containsExactly("Transaction made during unusual hours");
        }

        @Test
        @DisplayName("Transaction exactly at 5 AM is NOT considered unusual hours")
        void exactlyFiveAM_isNotFlagged() {
            TransactionRequest req = request(BigDecimal.valueOf(100),
                    "USA", LocalDateTime.of(2025, 1, 1, 5, 0), "PAYEE-1");

            Transaction result = service.processTransaction(req);

            assertThat(result.status()).isEqualTo("SUCCESS");
            assertThat(result.riskScore()).isZero();
        }

        @Test
        @DisplayName("Country mismatch AND unusual hours together accumulate risk score (30 + 20)")
        void countryMismatchAndUnusualHours_accumulateRisk() {
            TransactionRequest req = request(BigDecimal.valueOf(100),
                    "Germany", LocalDateTime.of(2025, 1, 1, 2, 0), "PAYEE-1");

            Transaction result = service.processTransaction(req);

            assertThat(result.status()).isEqualTo("FLAGGED");
            assertThat(result.riskScore()).isEqualTo(50);
            assertThat(result.reasons()).hasSize(2);
        }
    }

    // ---------------------------------------------------------------------
    // Dynamic rules engine
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("AMOUNT_THRESHOLD rule")
    class AmountThresholdRule {

        @Test
        @DisplayName("Amount exceeding threshold triggers the rule (+40 risk) and creates an alert referencing the rule")
        void amountAboveThreshold_triggersRule() {
            when(repository.findAllRules()).thenReturn(List.of(
                    rule(10, "AMOUNT_THRESHOLD", "{\"threshold\": 1000}", true)));

            TransactionRequest req = request(BigDecimal.valueOf(5000),
                    "USA", LocalDateTime.of(2025, 1, 1, 14, 0), "PAYEE-1");

            Transaction result = service.processTransaction(req);

            assertThat(result.status()).isEqualTo("FLAGGED");
            assertThat(result.riskScore()).isEqualTo(40);

            ArgumentCaptor<FraudAlert> alertCaptor = ArgumentCaptor.forClass(FraudAlert.class);
            verify(repository).saveAlert(alertCaptor.capture());
            assertThat(alertCaptor.getValue().ruleId()).isEqualTo(10);
        }

        @Test
        @DisplayName("Amount at or below threshold does not trigger the rule")
        void amountAtOrBelowThreshold_doesNotTrigger() {
            when(repository.findAllRules()).thenReturn(List.of(
                    rule(10, "AMOUNT_THRESHOLD", "{\"threshold\": 1000}", true)));

            TransactionRequest req = request(BigDecimal.valueOf(1000),
                    "USA", LocalDateTime.of(2025, 1, 1, 14, 0), "PAYEE-1");

            Transaction result = service.processTransaction(req);

            assertThat(result.status()).isEqualTo("SUCCESS");
            assertThat(result.riskScore()).isZero();
            verify(repository, never()).saveAlert(any());
        }

        @Test
        @DisplayName("Inactive rules are never evaluated even if they would otherwise trigger")
        void inactiveRule_isSkipped() {
            when(repository.findAllRules()).thenReturn(List.of(
                    rule(10, "AMOUNT_THRESHOLD", "{\"threshold\": 100}", false)));

            TransactionRequest req = request(BigDecimal.valueOf(5000),
                    "USA", LocalDateTime.of(2025, 1, 1, 14, 0), "PAYEE-1");

            Transaction result = service.processTransaction(req);

            assertThat(result.status()).isEqualTo("SUCCESS");
            assertThat(result.riskScore()).isZero();
        }

        @Test
        @DisplayName("Malformed rule parameters do not crash the transaction flow; rule is simply skipped")
        void malformedRuleParameters_areSkippedGracefully() {
            when(repository.findAllRules()).thenReturn(List.of(
                    rule(10, "AMOUNT_THRESHOLD", "{not-valid-json", true)));

            TransactionRequest req = request(BigDecimal.valueOf(5000),
                    "USA", LocalDateTime.of(2025, 1, 1, 14, 0), "PAYEE-1");

            Transaction result = service.processTransaction(req);

            assertThat(result.status()).isEqualTo("SUCCESS");
            assertThat(result.riskScore()).isZero();
        }
    }

    @Nested
    @DisplayName("VELOCITY rule")
    class VelocityRule {

        @Test
        @DisplayName("Transaction count exceeding max within the time window triggers the rule (+40 risk)")
        void tooManyRecentTransactions_triggersRule() {
            when(repository.findAllRules()).thenReturn(List.of(
                    rule(20, "VELOCITY", "{\"timeWindowMinutes\": 10, \"maxCount\": 3}", true)));
            when(repository.countRecentTransactions(eq(1), any(LocalDateTime.class))).thenReturn(5);

            TransactionRequest req = request(BigDecimal.valueOf(100),
                    "USA", LocalDateTime.of(2025, 1, 1, 14, 0), "PAYEE-1");

            Transaction result = service.processTransaction(req);

            assertThat(result.status()).isEqualTo("FLAGGED");
            assertThat(result.riskScore()).isEqualTo(40);
            assertThat(result.reasons().get(0)).contains("Velocity limit exceeded");
        }

        @Test
        @DisplayName("Transaction count within limit does not trigger the rule")
        void withinVelocityLimit_doesNotTrigger() {
            when(repository.findAllRules()).thenReturn(List.of(
                    rule(20, "VELOCITY", "{\"timeWindowMinutes\": 10, \"maxCount\": 3}", true)));
            when(repository.countRecentTransactions(eq(1), any(LocalDateTime.class))).thenReturn(2);

            TransactionRequest req = request(BigDecimal.valueOf(100),
                    "USA", LocalDateTime.of(2025, 1, 1, 14, 0), "PAYEE-1");

            Transaction result = service.processTransaction(req);

            assertThat(result.status()).isEqualTo("SUCCESS");
            assertThat(result.riskScore()).isZero();
        }
    }

    @Nested
    @DisplayName("NEW_PAYEE rule")
    class NewPayeeRule {

        @Test
        @DisplayName("Paying an unseen payee triggers the rule (+20 risk)")
        void unseenPayee_triggersRule() {
            when(repository.findAllRules()).thenReturn(List.of(
                    rule(30, "NEW_PAYEE", "{}", true)));
            when(repository.hasPaidPayeeBefore(eq(1), eq("NEW-PAYEE"), anyInt())).thenReturn(false);

            TransactionRequest req = request(BigDecimal.valueOf(100),
                    "USA", LocalDateTime.of(2025, 1, 1, 14, 0), "NEW-PAYEE");

            Transaction result = service.processTransaction(req);

            assertThat(result.status()).isEqualTo("FLAGGED");
            assertThat(result.riskScore()).isEqualTo(20);
            assertThat(result.reasons().get(0)).contains("Unseen payee counterparty detected");
        }

        @Test
        @DisplayName("Paying a previously-paid payee does not trigger the rule")
        void knownPayee_doesNotTrigger() {
            when(repository.findAllRules()).thenReturn(List.of(
                    rule(30, "NEW_PAYEE", "{}", true)));
            when(repository.hasPaidPayeeBefore(eq(1), eq("KNOWN-PAYEE"), anyInt())).thenReturn(true);

            TransactionRequest req = request(BigDecimal.valueOf(100),
                    "USA", LocalDateTime.of(2025, 1, 1, 14, 0), "KNOWN-PAYEE");

            Transaction result = service.processTransaction(req);

            assertThat(result.status()).isEqualTo("SUCCESS");
            assertThat(result.riskScore()).isZero();
        }
    }

    @Nested
    @DisplayName("DAILY_LIMIT rule")
    class DailyLimitRule {

        @Test
        @DisplayName("Cumulative daily total exceeding the limit triggers the rule (+50 risk)")
        void dailyTotalExceedsLimit_triggersRule() {
            when(repository.findAllRules()).thenReturn(List.of(
                    rule(40, "DAILY_LIMIT", "{\"dailyLimit\": 5000}", true)));
            when(repository.getDailyTransactionTotal(eq(1), any(LocalDate.class)))
                    .thenReturn(BigDecimal.valueOf(6000));

            TransactionRequest req = request(BigDecimal.valueOf(100),
                    "USA", LocalDateTime.of(2025, 1, 1, 14, 0), "PAYEE-1");

            Transaction result = service.processTransaction(req);

            assertThat(result.status()).isEqualTo("FLAGGED");
            assertThat(result.riskScore()).isEqualTo(50);
            assertThat(result.reasons().get(0)).contains("Daily accumulation total");
        }

        @Test
        @DisplayName("Cumulative daily total within the limit does not trigger the rule")
        void dailyTotalWithinLimit_doesNotTrigger() {
            when(repository.findAllRules()).thenReturn(List.of(
                    rule(40, "DAILY_LIMIT", "{\"dailyLimit\": 5000}", true)));
            when(repository.getDailyTransactionTotal(eq(1), any(LocalDate.class)))
                    .thenReturn(BigDecimal.valueOf(1000));

            TransactionRequest req = request(BigDecimal.valueOf(100),
                    "USA", LocalDateTime.of(2025, 1, 1, 14, 0), "PAYEE-1");

            Transaction result = service.processTransaction(req);

            assertThat(result.status()).isEqualTo("SUCCESS");
            assertThat(result.riskScore()).isZero();
        }
    }

    // ---------------------------------------------------------------------
    // Combined scenarios / risk scoring / severity
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("Combined risk scenarios and alert severity")
    class CombinedScenarios {

        @Test
        @DisplayName("Multiple triggered rules accumulate risk score and all reasons are recorded")
        void multipleRulesTriggered_accumulateRiskAndReasons() {
            when(repository.findAllRules()).thenReturn(List.of(
                    rule(10, "AMOUNT_THRESHOLD", "{\"threshold\": 1000}", true),
                    rule(20, "VELOCITY", "{\"timeWindowMinutes\": 10, \"maxCount\": 3}", true)));
            when(repository.countRecentTransactions(eq(1), any(LocalDateTime.class))).thenReturn(10);

            TransactionRequest req = request(BigDecimal.valueOf(5000),
                    "USA", LocalDateTime.of(2025, 1, 1, 14, 0), "PAYEE-1");

            Transaction result = service.processTransaction(req);

            assertThat(result.status()).isEqualTo("FLAGGED");
            assertThat(result.riskScore()).isEqualTo(80); // 40 + 40
            assertThat(result.reasons()).hasSize(2);
        }

        @Test
        @DisplayName("Alert's ruleId references the first rule that triggered, in evaluation order")
        void alert_referencesFirstTriggeredRule() {
            when(repository.findAllRules()).thenReturn(List.of(
                    rule(10, "AMOUNT_THRESHOLD", "{\"threshold\": 1000}", true),
                    rule(20, "VELOCITY", "{\"timeWindowMinutes\": 10, \"maxCount\": 3}", true)));
            when(repository.countRecentTransactions(eq(1), any(LocalDateTime.class))).thenReturn(10);

            TransactionRequest req = request(BigDecimal.valueOf(5000),
                    "USA", LocalDateTime.of(2025, 1, 1, 14, 0), "PAYEE-1");

            service.processTransaction(req);

            ArgumentCaptor<FraudAlert> captor = ArgumentCaptor.forClass(FraudAlert.class);
            verify(repository).saveAlert(captor.capture());
            assertThat(captor.getValue().ruleId()).isEqualTo(10);
        }

        @Test
        @DisplayName("Risk score below 30 results in LOW severity alert")
        void lowRiskScore_producesLowSeverity() {
            TransactionRequest req = request(BigDecimal.valueOf(100),
                    "USA", LocalDateTime.of(2025, 1, 1, 3, 0), "PAYEE-1"); // unusual hours only -> 20

            service.processTransaction(req);

            ArgumentCaptor<FraudAlert> captor = ArgumentCaptor.forClass(FraudAlert.class);
            verify(repository).saveAlert(captor.capture());
            assertThat(captor.getValue().severity()).isEqualTo("LOW");
        }

        @Test
        @DisplayName("Risk score between 30 and 59 results in MEDIUM severity alert")
        void mediumRiskScore_producesMediumSeverity() {
            TransactionRequest req = request(BigDecimal.valueOf(100),
                    "United Kingdom", LocalDateTime.of(2025, 1, 1, 14, 0), "PAYEE-1"); // country mismatch -> 30

            service.processTransaction(req);

            ArgumentCaptor<FraudAlert> captor = ArgumentCaptor.forClass(FraudAlert.class);
            verify(repository).saveAlert(captor.capture());
            assertThat(captor.getValue().severity()).isEqualTo("MEDIUM");
        }

        @Test
        @DisplayName("Risk score of 60 or more results in HIGH severity alert")
        void highRiskScore_producesHighSeverity() {
            TransactionRequest req = request(BigDecimal.valueOf(100),
                    "United Kingdom", LocalDateTime.of(2025, 1, 1, 2, 0), "PAYEE-1"); // 30 + 20 = 50... need 60+

            when(repository.findAllRules()).thenReturn(List.of(
                    rule(30, "NEW_PAYEE", "{}", true)));
            when(repository.hasPaidPayeeBefore(eq(1), eq("PAYEE-1"), anyInt())).thenReturn(false);

            service.processTransaction(req); // 30 + 20 + 20 = 70

            ArgumentCaptor<FraudAlert> captor = ArgumentCaptor.forClass(FraudAlert.class);
            verify(repository).saveAlert(captor.capture());
            assertThat(captor.getValue().riskScore()).isEqualTo(70);
            assertThat(captor.getValue().severity()).isEqualTo("HIGH");
        }
    }

    // ---------------------------------------------------------------------
    // Transaction lifecycle / persistence behavior
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("Transaction persistence lifecycle")
    class PersistenceLifecycle {

        @Test
        @DisplayName("A placeholder PENDING transaction is saved first, then deleted, then replaced by the final record")
        void placeholderTransaction_isReplacedByFinalRecord() {
            TransactionRequest req = request(BigDecimal.valueOf(5000),
                    "United Kingdom", LocalDateTime.of(2025, 1, 1, 14, 0), "PAYEE-1");

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            service.processTransaction(req);

            verify(repository, times(2)).saveTransaction(captor.capture());
            Transaction placeholder = captor.getAllValues().get(0);
            Transaction finalTx = captor.getAllValues().get(1);

            assertThat(placeholder.status()).isEqualTo("PENDING");
            assertThat(placeholder.riskScore()).isZero();
            assertThat(finalTx.status()).isEqualTo("FLAGGED");
            assertThat(finalTx.riskScore()).isEqualTo(30);

            verify(repository).deleteTransaction(anyInt());
        }

        @Test
        @DisplayName("Unknown customer id causes a 400 Bad Request and no transaction is persisted")
        void unknownCustomer_throwsBadRequest() {
            when(repository.findCustomer(999)).thenReturn(Optional.empty());
            TransactionRequest req = new TransactionRequest(999, BigDecimal.TEN, "PAYEE-1", "USA",
                    LocalDateTime.of(2025, 1, 1, 14, 0), "desc");

            assertThatThrownBy(() -> service.processTransaction(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Customer not found");

            verify(repository, never()).saveTransaction(any());
            verify(repository, never()).saveAlert(any());
        }
    }

    // ---------------------------------------------------------------------
    // Alert lifecycle / status transitions
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("Alert status transitions")
    class AlertStatusTransitions {

        private FraudAlert openAlert() {
            return new FraudAlert(1, 100, 10, "Alice Doe", "MEDIUM", "OPEN", 30,
                    List.of("Some reason"), "", LocalDateTime.now(), LocalDateTime.now());
        }

        @Test
        @DisplayName("OPEN -> ACKNOWLEDGED is a valid transition")
        void openToAcknowledged_isValid() {
            when(repository.findAlert(1)).thenReturn(Optional.of(openAlert()));

            FraudAlert result = service.updateAlertStatus(1, "ACKNOWLEDGED", "reviewing");

            assertThat(result.status()).isEqualTo("ACKNOWLEDGED");
            verify(repository).updateAlert(any(FraudAlert.class));
            verify(repository).saveAlertHistory(eq(1), eq("ACKNOWLEDGED"), eq("reviewing"));
        }

        @Test
        @DisplayName("OPEN -> CLOSED directly is an invalid transition")
        void openToClosed_isInvalid() {
            when(repository.findAlert(1)).thenReturn(Optional.of(openAlert()));

            assertThatThrownBy(() -> service.updateAlertStatus(1, "CLOSED", "skip investigation"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid status transition");

            verify(repository, never()).updateAlert(any());
        }

        @Test
        @DisplayName("Setting the same status is a no-op and does not persist changes")
        void sameStatus_isNoOp() {
            when(repository.findAlert(1)).thenReturn(Optional.of(openAlert()));

            FraudAlert result = service.updateAlertStatus(1, "OPEN", "no change");

            assertThat(result.status()).isEqualTo("OPEN");
            verify(repository, never()).updateAlert(any());
            verify(repository, never()).saveAlertHistory(anyInt(), any(), any());
        }

        @Test
        @DisplayName("Unknown alert id results in 404 Not Found")
        void unknownAlert_throwsNotFound() {
            when(repository.findAlert(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateAlertStatus(99, "ACKNOWLEDGED", "notes"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Alert not found");
        }
    }

    // ---------------------------------------------------------------------
    // Ancillary CRUD / read pass-through methods
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("Customer management")
    class CustomerManagement {

        @Test
        @DisplayName("getCustomers delegates to repository.findAllCustomers()")
        void getCustomers_delegatesToRepository() {
            when(repository.findAllCustomers()).thenReturn(List.of(customer));

            assertThat(service.getCustomers()).containsExactly(customer);
        }

        @Test
        @DisplayName("getCustomer returns the customer when found")
        void getCustomer_found_returnsCustomer() {
            assertThat(service.getCustomer(1)).isEqualTo(customer);
        }

        @Test
        @DisplayName("getCustomer throws 404 when not found")
        void getCustomer_notFound_throwsNotFound() {
            when(repository.findCustomer(404)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCustomer(404))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Customer not found");
        }

        @Test
        @DisplayName("createCustomer delegates to repository.saveCustomer(customer)")
        void createCustomer_delegatesToRepository() {
            Customer input = new Customer(0, "Bob", "ACC-2", "India");
            Customer saved = new Customer(2, "Bob", "ACC-2", "India");
            when(repository.saveCustomer(input)).thenReturn(saved);

            assertThat(service.createCustomer(input)).isEqualTo(saved);
        }

        @Test
        @DisplayName("updateCustomer checks existence first, then delegates to repository.updateCustomer")
        void updateCustomer_existingCustomer_updatesFields() {
            Customer updateData = new Customer(0, "Alice Updated", "ACC-999", "Canada");
            when(repository.updateCustomer(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            Customer result = service.updateCustomer(1, updateData);

            assertThat(result.id()).isEqualTo(1);
            assertThat(result.name()).isEqualTo("Alice Updated");
        }

        @Test
        @DisplayName("updateCustomer throws 404 when the customer does not exist")
        void updateCustomer_unknownCustomer_throwsNotFound() {
            when(repository.findCustomer(404)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateCustomer(404, customer))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("deleteCustomer checks existence then delegates to repository.deleteCustomer")
        void deleteCustomer_existingCustomer_deletes() {
            service.deleteCustomer(1);

            verify(repository).deleteCustomer(1);
        }

        @Test
        @DisplayName("deleteCustomer throws 404 when the customer does not exist")
        void deleteCustomer_unknownCustomer_throwsNotFound() {
            when(repository.findCustomer(404)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteCustomer(404))
                    .isInstanceOf(ResponseStatusException.class);
            verify(repository, never()).deleteCustomer(anyInt());
        }
    }

    @Nested
    @DisplayName("Transaction and alert read/delete operations")
    class TransactionAndAlertReads {

        @Test
        @DisplayName("getTransactions delegates to repository.findAllTransactions()")
        void getTransactions_delegatesToRepository() {
            Transaction transaction = new Transaction(1, 1, "Alice", BigDecimal.TEN, "P", "USA",
                    LocalDateTime.now(), "d", "SUCCESS", 0, List.of());
            when(repository.findAllTransactions()).thenReturn(List.of(transaction));

            assertThat(service.getTransactions()).containsExactly(transaction);
        }

        @Test
        @DisplayName("getTransaction throws 404 when not found")
        void getTransaction_notFound_throwsNotFound() {
            when(repository.findTransaction(404)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getTransaction(404))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Transaction not found");
        }

        @Test
        @DisplayName("deleteTransaction checks existence then delegates to repository.deleteTransaction")
        void deleteTransaction_existing_deletes() {
            Transaction transaction = new Transaction(5, 1, "Alice", BigDecimal.TEN, "P", "USA",
                    LocalDateTime.now(), "d", "SUCCESS", 0, List.of());
            when(repository.findTransaction(5)).thenReturn(Optional.of(transaction));

            service.deleteTransaction(5);

            verify(repository).deleteTransaction(5);
        }

        @Test
        @DisplayName("getAlerts delegates to repository.findAllAlerts()")
        void getAlerts_delegatesToRepository() {
            FraudAlert alert = new FraudAlert(1, 100, 10, "Alice", "HIGH", "OPEN", 70,
                    List.of("r"), "", LocalDateTime.now(), LocalDateTime.now());
            when(repository.findAllAlerts()).thenReturn(List.of(alert));

            assertThat(service.getAlerts()).containsExactly(alert);
        }

        @Test
        @DisplayName("getAlert throws 404 when not found")
        void getAlert_notFound_throwsNotFound() {
            when(repository.findAlert(404)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getAlert(404))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Alert not found");
        }

        @Test
        @DisplayName("getAlertHistory checks the alert exists then delegates to repository.findAlertHistory")
        void getAlertHistory_delegatesToRepository() {
            FraudAlert alert = new FraudAlert(1, 100, 10, "Alice", "HIGH", "OPEN", 70,
                    List.of("r"), "", LocalDateTime.now(), LocalDateTime.now());
            when(repository.findAlert(1)).thenReturn(Optional.of(alert));
            AlertHistoryItem historyItem = new AlertHistoryItem(1, 1, "OPEN", "created", LocalDateTime.now());
            when(repository.findAlertHistory(1)).thenReturn(List.of(historyItem));

            assertThat(service.getAlertHistory(1)).containsExactly(historyItem);
        }

        @Test
        @DisplayName("getAlertHistory throws 404 when the alert does not exist")
        void getAlertHistory_unknownAlert_throwsNotFound() {
            when(repository.findAlert(404)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getAlertHistory(404))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("getRecentActivity delegates to repository.findRecentActivity(100)")
        void getRecentActivity_delegatesToRepository() {
            AuditLogEntry entry = new AuditLogEntry(1, 1, 100, "Alice", "HIGH", "OPEN", "notes", LocalDateTime.now());
            when(repository.findRecentActivity(100)).thenReturn(List.of(entry));

            assertThat(service.getRecentActivity()).containsExactly(entry);
        }
    }

    @Nested
    @DisplayName("Dashboard summary")
    class DashboardSummaryTests {

        @Test
        @DisplayName("getSummary aggregates transaction volume, flagged count and open alert count")
        void getSummary_aggregatesCorrectly() {
            Transaction flagged = new Transaction(1, 1, "Alice", BigDecimal.valueOf(100), "P", "USA",
                    LocalDateTime.now(), "d", "FLAGGED", 30, List.of("reason"));
            Transaction success = new Transaction(2, 1, "Alice", BigDecimal.valueOf(200), "P", "USA",
                    LocalDateTime.now(), "d", "SUCCESS", 0, List.of());
            when(repository.findAllTransactions()).thenReturn(List.of(flagged, success));

            FraudAlert open = new FraudAlert(1, 1, 10, "Alice", "HIGH", "OPEN", 70,
                    List.of("r"), "", LocalDateTime.now(), LocalDateTime.now());
            FraudAlert closed = new FraudAlert(2, 2, 10, "Alice", "LOW", "CLOSED", 10,
                    List.of("r"), "resolved", LocalDateTime.now(), LocalDateTime.now());
            when(repository.findAllAlerts()).thenReturn(List.of(open, closed));

            DashboardSummary summary = service.getSummary();

            assertThat(summary.totalTransactions()).isEqualTo(2);
            assertThat(summary.flaggedTransactions()).isEqualTo(1);
            assertThat(summary.openAlerts()).isEqualTo(1);
            assertThat(summary.totalVolume()).isEqualByComparingTo(BigDecimal.valueOf(300));
        }

        @Test
        @DisplayName("getSummary converts non-USD transaction amounts using the country's exchange rate")
        void getSummary_convertsForeignCurrency() {
            // India -> INR at ~0.012 to USD
            Transaction indiaTx = new Transaction(1, 1, "Alice", BigDecimal.valueOf(1000), "P", "India",
                    LocalDateTime.now(), "d", "SUCCESS", 0, List.of());
            when(repository.findAllTransactions()).thenReturn(List.of(indiaTx));
            when(repository.findAllAlerts()).thenReturn(List.of());

            DashboardSummary summary = service.getSummary();

            assertThat(summary.totalVolume()).isEqualByComparingTo(BigDecimal.valueOf(12.0));
        }
    }

    @Nested
    @DisplayName("Rule management")
    class RuleManagement {

        @Test
        @DisplayName("getRules delegates to repository.findAllRules()")
        void getRules_delegatesToRepository() {
            Rule r = rule(1, "AMOUNT_THRESHOLD", "{}", true);
            when(repository.findAllRules()).thenReturn(List.of(r));

            assertThat(service.getRules()).containsExactly(r);
        }

        @Test
        @DisplayName("getRule throws 404 when not found")
        void getRule_notFound_throwsNotFound() {
            when(repository.findRule(404)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getRule(404))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Rule not found");
        }

        @Test
        @DisplayName("createRule delegates to repository.saveRule(rule)")
        void createRule_delegatesToRepository() {
            Rule input = rule(0, "VELOCITY", "{}", true);
            Rule saved = rule(1, "VELOCITY", "{}", true);
            when(repository.saveRule(input)).thenReturn(saved);

            assertThat(service.createRule(input)).isEqualTo(saved);
        }

        @Test
        @DisplayName("updateRule checks existence then delegates to repository.updateRule")
        void updateRule_existingRule_updatesFields() {
            Rule existing = rule(1, "AMOUNT_THRESHOLD", "{}", true);
            when(repository.findRule(1)).thenReturn(Optional.of(existing));
            Rule updateData = new Rule(0, "New Name", "AMOUNT_THRESHOLD", "HIGH", "{\"threshold\":1}", false);
            when(repository.updateRule(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));

            Rule result = service.updateRule(1, updateData);

            assertThat(result.id()).isEqualTo(1);
            assertThat(result.name()).isEqualTo("New Name");
        }

        @Test
        @DisplayName("updateRule throws 404 when the rule does not exist")
        void updateRule_unknownRule_throwsNotFound() {
            when(repository.findRule(404)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateRule(404, rule(0, "X", "{}", true)))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("deleteRule checks existence then delegates to repository.deleteRule")
        void deleteRule_existingRule_deletes() {
            Rule existing = rule(1, "AMOUNT_THRESHOLD", "{}", true);
            when(repository.findRule(1)).thenReturn(Optional.of(existing));

            service.deleteRule(1);

            verify(repository).deleteRule(1);
        }

        @Test
        @DisplayName("toggleRule flips the isActive flag and persists via repository.updateRule")
        void toggleRule_flipsActiveFlag() {
            Rule existing = rule(1, "AMOUNT_THRESHOLD", "{}", true);
            when(repository.findRule(1)).thenReturn(Optional.of(existing));
            when(repository.updateRule(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));

            Rule toggled = service.toggleRule(1);

            assertThat(toggled.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("Demo data seeding")
    class DemoSeeding {

        @Test
        @DisplayName("seedDemo skips reseeding when data already exists and force=false")
        void seedDemo_existingDataNotForced_skipsReseeding() {
            Transaction existing = new Transaction(1, 1, "Alice", BigDecimal.TEN, "P", "USA",
                    LocalDateTime.now(), "d", "SUCCESS", 0, List.of());
            when(repository.findAllTransactions()).thenReturn(List.of(existing));
            when(repository.findAllAlerts()).thenReturn(List.of());

            Map<String, Object> result = service.seedDemo(false);

            assertThat(result.get("skipped")).isEqualTo(true);
            verify(repository, never()).clearAllForDemo();
        }

        @Test
        @DisplayName("seedDemo(force=true) clears existing data and regenerates customers and transactions")
        void seedDemo_forced_regeneratesDemoData() {
            Map<Integer, Customer> customerStore = new ConcurrentHashMap<>();
            AtomicInteger customerIdSeq = new AtomicInteger(0);

            when(repository.saveCustomer(any(Customer.class))).thenAnswer(invocation -> {
                Customer input = invocation.getArgument(0);
                int id = customerIdSeq.incrementAndGet();
                Customer saved = new Customer(id, input.name(), input.accountNumber(), input.registeredCountry());
                customerStore.put(id, saved);
                return saved;
            });
            when(repository.findCustomer(anyInt())).thenAnswer(invocation -> {
                int id = invocation.getArgument(0);
                return Optional.ofNullable(customerStore.get(id));
            });
            when(repository.findAllTransactions()).thenReturn(List.of(
                    new Transaction(1, 1, "Alice", BigDecimal.TEN, "P", "USA", LocalDateTime.now(), "d", "SUCCESS", 0, List.of())));
            when(repository.findAllAlerts()).thenReturn(List.of());

            Map<String, Object> result = service.seedDemo(true);

            assertThat(result.get("skipped")).isEqualTo(false);
            verify(repository).clearAllForDemo();
            verify(repository, atLeast(10)).saveCustomer(any(Customer.class));
            verify(repository, atLeast(500)).saveTransaction(any(Transaction.class));
        }
    }
}
