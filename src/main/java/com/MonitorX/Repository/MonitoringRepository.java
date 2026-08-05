package com.MonitorX.Repository;

import com.MonitorX.models.Customer;
import com.MonitorX.models.FraudAlert;
import com.MonitorX.models.Transaction;
import com.MonitorX.models.Rule;
import com.MonitorX.models.AlertHistoryItem;
import com.MonitorX.models.AuditLogEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
public class MonitoringRepository {
    private final JdbcTemplate jdbc;

    public MonitoringRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Customer Row Mapper method
    private Customer mapCustomer(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new Customer(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("account_number"),
                rs.getString("registered_country")
        );
    }

    // Transaction Row Mapper method
    private Transaction mapTransaction(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        int id = rs.getInt("id");
        List<String> reasons = jdbc.queryForList(
                "SELECT reason FROM transaction_reasons WHERE transaction_id = ?", String.class, id);
        return new Transaction(
                id,
                rs.getInt("customer_id"),
                rs.getString("customer_name"),
                rs.getBigDecimal("amount"),
                rs.getString("payee_id"),
                rs.getString("transaction_country"),
                rs.getTimestamp("timestamp").toLocalDateTime(),
                rs.getString("description"),
                rs.getString("status"),
                rs.getInt("risk_score"),
                reasons
        );
    }

    // FraudAlert Row Mapper method
    private FraudAlert mapAlert(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        String reasonsStr = rs.getString("reasons");
        List<String> reasons = reasonsStr == null || reasonsStr.isEmpty()
                ? List.of()
                : Arrays.asList(reasonsStr.split("\\|"));
        
        Integer ruleId = rs.getInt("rule_id");
        if (rs.wasNull()) {
            ruleId = null;
        }

        return new FraudAlert(
                rs.getInt("id"),
                rs.getInt("transaction_id"),
                ruleId,
                rs.getString("customer_name"),
                rs.getString("severity"),
                rs.getString("status"),
                rs.getInt("risk_score"),
                reasons,
                rs.getString("resolution_notes"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }

    // Rule Row Mapper method
    private Rule mapRule(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new Rule(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("type"),
                rs.getString("severity"),
                rs.getString("parameters"),
                rs.getBoolean("is_active")
        );
    }

    // AlertHistoryItem Row Mapper method
    private AlertHistoryItem mapHistory(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new AlertHistoryItem(
                rs.getInt("id"),
                rs.getInt("alert_id"),
                rs.getString("status"),
                rs.getString("operator_notes"),
                rs.getTimestamp("changed_at").toLocalDateTime()
        );
    }

    // Customer Methods
    public List<Customer> findAllCustomers() {
        return jdbc.query("SELECT id, name, account_number, registered_country FROM customers ORDER BY id", this::mapCustomer);
    }

    public Optional<Customer> findCustomer(int id) {
        List<Customer> results = jdbc.query("SELECT id, name, account_number, registered_country FROM customers WHERE id = ?", this::mapCustomer, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Customer saveCustomer(Customer customer) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO customers (name, account_number, registered_country) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, customer.name());
            ps.setString(2, customer.accountNumber());
            ps.setString(3, customer.registeredCountry());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        int id = key != null ? key.intValue() : 0;
        return new Customer(id, customer.name(), customer.accountNumber(), customer.registeredCountry());
    }

    public Customer updateCustomer(Customer customer) {
        jdbc.update(
                "UPDATE customers SET name = ?, account_number = ?, registered_country = ? WHERE id = ?",
                customer.name(),
                customer.accountNumber(),
                customer.registeredCountry(),
                customer.id()
        );
        return customer;
    }

    public void deleteCustomer(int id) {
        jdbc.update("DELETE FROM customers WHERE id = ?", id);
    }

    // Operator Methods
    public Optional<String> getOperatorPasswordHash(String username) {
        List<String> results = jdbc.query(
                "SELECT password_hash FROM operators WHERE username = ?",
                (rs, rowNum) -> rs.getString("password_hash"),
                username
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    // Transaction Methods
    public Transaction saveTransaction(Transaction transaction) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO transactions (customer_id, amount, payee_id, transaction_country, timestamp, description, status, risk_score) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setInt(1, transaction.customerId());
            ps.setBigDecimal(2, transaction.amount());
            ps.setString(3, transaction.payeeId());
            ps.setString(4, transaction.transactionCountry());
            ps.setTimestamp(5, Timestamp.valueOf(transaction.timestamp()));
            ps.setString(6, transaction.description() == null ? "" : transaction.description());
            ps.setString(7, transaction.status());
            ps.setInt(8, transaction.riskScore());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        int id = key != null ? key.intValue() : 0;

        for (String reason : transaction.reasons()) {
            jdbc.update("INSERT INTO transaction_reasons (transaction_id, reason) VALUES (?, ?)", id, reason);
        }

        return new Transaction(
                id,
                transaction.customerId(),
                transaction.customerName(),
                transaction.amount(),
                transaction.payeeId(),
                transaction.transactionCountry(),
                transaction.timestamp(),
                transaction.description(),
                transaction.status(),
                transaction.riskScore(),
                transaction.reasons()
        );
    }

    public List<Transaction> findAllTransactions() {
        return jdbc.query(
                "SELECT t.id, t.customer_id, c.name as customer_name, t.amount, t.payee_id, t.transaction_country, t.timestamp, t.description, t.status, t.risk_score " +
                "FROM transactions t JOIN customers c ON t.customer_id = c.id ORDER BY t.timestamp DESC",
                this::mapTransaction
        );
    }

    public Optional<Transaction> findTransaction(int id) {
        List<Transaction> results = jdbc.query(
                "SELECT t.id, t.customer_id, c.name as customer_name, t.amount, t.payee_id, t.transaction_country, t.timestamp, t.description, t.status, t.risk_score " +
                "FROM transactions t JOIN customers c ON t.customer_id = c.id WHERE t.id = ?",
                this::mapTransaction,
                id
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public void deleteTransaction(int id) {
        jdbc.update("DELETE FROM transactions WHERE id = ?", id);
    }

    // Alert Methods
    public FraudAlert saveAlert(FraudAlert alert) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO alerts (transaction_id, rule_id, customer_name, severity, status, risk_score, reasons, resolution_notes, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setInt(1, alert.transactionId());
            if (alert.ruleId() != null) {
                ps.setInt(2, alert.ruleId());
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            ps.setString(3, alert.customerName());
            ps.setString(4, alert.severity());
            ps.setString(5, alert.status());
            ps.setInt(6, alert.riskScore());
            ps.setString(7, String.join("|", alert.reasons()));
            ps.setString(8, alert.resolutionNotes() == null ? "" : alert.resolutionNotes());
            ps.setTimestamp(9, Timestamp.valueOf(alert.createdAt()));
            ps.setTimestamp(10, Timestamp.valueOf(alert.updatedAt() == null ? LocalDateTime.now() : alert.updatedAt()));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        int id = key != null ? key.intValue() : 0;

        saveAlertHistory(id, alert.status(), "Alert generated automatically by rule evaluation.");

        return new FraudAlert(
                id,
                alert.transactionId(),
                alert.ruleId(),
                alert.customerName(),
                alert.severity(),
                alert.status(),
                alert.riskScore(),
                alert.reasons(),
                alert.resolutionNotes(),
                alert.createdAt(),
                alert.updatedAt() == null ? LocalDateTime.now() : alert.updatedAt()
        );
    }

    public List<FraudAlert> findAllAlerts() {
        return jdbc.query(
                "SELECT id, transaction_id, rule_id, customer_name, severity, status, risk_score, reasons, resolution_notes, created_at, updated_at FROM alerts ORDER BY created_at DESC",
                this::mapAlert
        );
    }

    public Optional<FraudAlert> findAlert(int id) {
        List<FraudAlert> results = jdbc.query(
                "SELECT id, transaction_id, rule_id, customer_name, severity, status, risk_score, reasons, resolution_notes, created_at, updated_at FROM alerts WHERE id = ?",
                this::mapAlert,
                id
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public FraudAlert updateAlert(FraudAlert alert) {
        jdbc.update(
                "UPDATE alerts SET status = ?, resolution_notes = ?, updated_at = ? WHERE id = ?",
                alert.status(),
                alert.resolutionNotes(),
                Timestamp.valueOf(alert.updatedAt()),
                alert.id()
        );
        return alert;
    }

    // Alert History Methods
    public List<AlertHistoryItem> findAlertHistory(int alertId) {
        return jdbc.query(
                "SELECT id, alert_id, status, operator_notes, changed_at FROM alert_history WHERE alert_id = ? ORDER BY changed_at ASC",
                this::mapHistory,
                alertId
        );
    }

    public void saveAlertHistory(int alertId, String status, String notes) {
        jdbc.update(
                "INSERT INTO alert_history (alert_id, status, operator_notes, changed_at) VALUES (?, ?, ?, ?)",
                alertId,
                status,
                notes == null ? "" : notes,
                Timestamp.valueOf(LocalDateTime.now())
        );
    }

    public List<AuditLogEntry> findRecentActivity(int limit) {
        return jdbc.query(
                "SELECT ah.id, ah.alert_id, a.transaction_id, a.customer_name, a.severity, " +
                "ah.status, ah.operator_notes, ah.changed_at " +
                "FROM alert_history ah " +
                "JOIN alerts a ON ah.alert_id = a.id " +
                "ORDER BY ah.changed_at DESC LIMIT ?",
                (rs, rowNum) -> new AuditLogEntry(
                        rs.getInt("id"),
                        rs.getInt("alert_id"),
                        rs.getInt("transaction_id"),
                        rs.getString("customer_name"),
                        rs.getString("severity"),
                        rs.getString("status"),
                        rs.getString("operator_notes"),
                        rs.getTimestamp("changed_at").toLocalDateTime()
                ),
                limit
        );
    }

    // Rule Methods
    public List<Rule> findAllRules() {
        return jdbc.query("SELECT id, name, type, severity, parameters, is_active FROM rules ORDER BY id", this::mapRule);
    }

    public Optional<Rule> findRule(int id) {
        List<Rule> results = jdbc.query("SELECT id, name, type, severity, parameters, is_active FROM rules WHERE id = ?", this::mapRule, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Rule saveRule(Rule rule) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO rules (name, type, severity, parameters, is_active) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, rule.name());
            ps.setString(2, rule.type());
            ps.setString(3, rule.severity());
            ps.setString(4, rule.parameters());
            ps.setBoolean(5, rule.isActive());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        int id = key != null ? key.intValue() : 0;
        return new Rule(id, rule.name(), rule.type(), rule.severity(), rule.parameters(), rule.isActive());
    }

    public Rule updateRule(Rule rule) {
        jdbc.update(
                "UPDATE rules SET name = ?, type = ?, severity = ?, parameters = ?, is_active = ? WHERE id = ?",
                rule.name(),
                rule.type(),
                rule.severity(),
                rule.parameters(),
                rule.isActive(),
                rule.id()
        );
        return rule;
    }

    public void deleteRule(int id) {
        jdbc.update("DELETE FROM rules WHERE id = ?", id);
    }

    // Rule Engine Helper Queries
    public int countRecentTransactions(int customerId, LocalDateTime since) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE customer_id = ? AND timestamp >= ?",
                Integer.class,
                customerId,
                Timestamp.valueOf(since)
        );
        return count != null ? count : 0;
    }

    public boolean hasPaidPayeeBefore(int customerId, String payeeId, int currentTxId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE customer_id = ? AND payee_id = ? AND id != ?",
                Integer.class,
                customerId,
                payeeId,
                currentTxId
        );
        return count != null && count > 0;
    }

    public BigDecimal getDailyTransactionTotal(int customerId, LocalDate date) {
        BigDecimal total = jdbc.queryForObject(
                "SELECT SUM(amount) FROM transactions WHERE customer_id = ? AND CAST(timestamp AS DATE) = ?",
                BigDecimal.class,
                customerId,
                java.sql.Date.valueOf(date)
        );
        return total != null ? total : BigDecimal.ZERO;
    }

    public void clearActivity() {
        jdbc.update("DELETE FROM alert_history");
        jdbc.update("DELETE FROM alerts");
        jdbc.update("DELETE FROM transaction_reasons");
        jdbc.update("DELETE FROM transactions");
        // Reset auto-increment counters (MySQL syntax)
        jdbc.update("ALTER TABLE transactions AUTO_INCREMENT = 1");
        jdbc.update("ALTER TABLE alerts AUTO_INCREMENT = 1");
        jdbc.update("ALTER TABLE alert_history AUTO_INCREMENT = 1");
    }
}
