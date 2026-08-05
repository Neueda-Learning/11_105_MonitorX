package com.MonitorX.Services;

import com.MonitorX.Repository.MonitoringRepository;
import com.MonitorX.models.Customer;
import com.MonitorX.models.DashboardSummary;
import com.MonitorX.models.FraudAlert;
import com.MonitorX.models.Transaction;
import com.MonitorX.models.TransactionRequest;
import com.MonitorX.models.Rule;
import com.MonitorX.models.AlertHistoryItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class FraudDetectionService {
    private final MonitoringRepository repository;
    private final ObjectMapper objectMapper;

    public FraudDetectionService(MonitoringRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public List<Customer> getCustomers() {
        return repository.findAllCustomers();
    }

    public Customer getCustomer(int id) {
        return repository.findCustomer(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
    }

    public Customer createCustomer(Customer customer) {
        return repository.saveCustomer(customer);
    }

    public Customer updateCustomer(int id, Customer customer) {
        getCustomer(id); // Check existence
        Customer toUpdate = new Customer(id, customer.name(), customer.accountNumber(), customer.registeredCountry());
        return repository.updateCustomer(toUpdate);
    }

    public void deleteCustomer(int id) {
        getCustomer(id); // Check existence
        repository.deleteCustomer(id);
    }

    public List<Transaction> getTransactions() {
        return repository.findAllTransactions();
    }

    public Transaction getTransaction(int id) {
        return repository.findTransaction(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    public Transaction processTransaction(TransactionRequest request) {
        Customer customer = repository.findCustomer(request.customerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer not found"));

        LocalDateTime timestamp = request.timestamp() == null ? LocalDateTime.now() : request.timestamp();
        String country = request.transactionCountry().trim();
        String payeeId = request.payeeId() == null ? "" : request.payeeId().trim();

        List<String> reasons = new ArrayList<>();
        int riskScore = 0;

        // 1. Built-in location check
        if (!customer.registeredCountry().equalsIgnoreCase(country)) {
            reasons.add("Transaction country differs from customer country");
            riskScore += 30;
        }

        // 2. Built-in unusual hours check
        if (timestamp.getHour() < 5) {
            reasons.add("Transaction made during unusual hours");
            riskScore += 20;
        }

        // Save transaction with placeholder status so we can query history in rules evaluation
        Transaction placeholder = repository.saveTransaction(new Transaction(
                0, customer.id(), customer.name(), request.amount(), payeeId, country, timestamp,
                request.description() == null ? "" : request.description().trim(),
                "PENDING", 0, List.of()
        ));

        // 3. Dynamic Rules Engine Evaluation
        List<Rule> activeRules = repository.findAllRules().stream()
                .filter(Rule::isActive)
                .toList();

        Integer triggeredRuleId = null;

        for (Rule rule : activeRules) {
            boolean triggered = false;
            String ruleReason = "";
            int ruleRisk = 0;

            try {
                Map<String, Object> params = objectMapper.readValue(rule.parameters(), Map.class);
                switch (rule.type().toUpperCase(Locale.ROOT)) {
                    case "AMOUNT_THRESHOLD":
                        if (params.containsKey("threshold")) {
                            BigDecimal threshold = new BigDecimal(params.get("threshold").toString());
                            if (request.amount().compareTo(threshold) > 0) {
                                triggered = true;
                                ruleReason = "Amount " + request.amount() + " exceeds threshold of " + threshold;
                                ruleRisk = 40;
                            }
                        }
                        break;

                    case "VELOCITY":
                        if (params.containsKey("timeWindowMinutes") && params.containsKey("maxCount")) {
                            int minutes = ((Number) params.get("timeWindowMinutes")).intValue();
                            int maxCount = ((Number) params.get("maxCount")).intValue();
                            LocalDateTime since = timestamp.minusMinutes(minutes);
                            int txCount = repository.countRecentTransactions(customer.id(), since);
                            // If transaction count exceeds the limit
                            if (txCount > maxCount) {
                                triggered = true;
                                ruleReason = "Velocity limit exceeded: " + txCount + " transactions in last " + minutes + " minutes (limit " + maxCount + ")";
                                ruleRisk = 40;
                            }
                        }
                        break;

                    case "NEW_PAYEE":
                        // Check if the payee has been paid before by this customer
                        boolean paidBefore = repository.hasPaidPayeeBefore(customer.id(), payeeId, placeholder.id());
                        if (!paidBefore) {
                            triggered = true;
                            ruleReason = "Unseen payee counterparty detected: " + payeeId;
                            ruleRisk = 20;
                        }
                        break;

                    case "DAILY_LIMIT":
                        if (params.containsKey("dailyLimit")) {
                            BigDecimal dailyLimit = new BigDecimal(params.get("dailyLimit").toString());
                            BigDecimal dailyTotal = repository.getDailyTransactionTotal(customer.id(), timestamp.toLocalDate());
                            if (dailyTotal.compareTo(dailyLimit) > 0) {
                                triggered = true;
                                ruleReason = "Daily accumulation total of " + dailyTotal + " exceeds limit of " + dailyLimit;
                                ruleRisk = 50;
                            }
                        }
                        break;
                }
            } catch (Exception e) {
                // If parsing fails, log and continue to avoid crashing transaction flow
                System.err.println("Error evaluating rule " + rule.name() + ": " + e.getMessage());
            }

            if (triggered) {
                reasons.add("Rule [" + rule.name() + "]: " + ruleReason);
                riskScore += ruleRisk;
                if (triggeredRuleId == null) {
                    triggeredRuleId = rule.id();
                }
            }
        }

        // Determine final transaction status
        String finalStatus = reasons.isEmpty() ? "SUCCESS" : "FLAGGED";

        // Delete placeholder and write finalized transaction with its reasons and risk score
        repository.deleteTransaction(placeholder.id());
        Transaction finalized = repository.saveTransaction(new Transaction(
                0, customer.id(), customer.name(), request.amount(), payeeId, country, timestamp,
                request.description() == null ? "" : request.description().trim(),
                finalStatus, riskScore, List.copyOf(reasons)
        ));

        // Create alert if flagged
        if (!reasons.isEmpty()) {
            repository.saveAlert(new FraudAlert(
                    0, finalized.id(), triggeredRuleId, customer.name(), severity(riskScore), "OPEN",
                    riskScore, List.copyOf(reasons), "", LocalDateTime.now(), LocalDateTime.now()
            ));
        }

        return finalized;
    }

    public void deleteTransaction(int id) {
        getTransaction(id);
        repository.deleteTransaction(id);
    }

    public List<FraudAlert> getAlerts() {
        return repository.findAllAlerts();
    }

    public FraudAlert getAlert(int id) {
        return repository.findAlert(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));
    }

    public FraudAlert updateAlertStatus(int id, String requestedStatus, String notes) {
        FraudAlert current = repository.findAlert(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));

        String next = requestedStatus == null ? "" : requestedStatus.trim().toUpperCase(Locale.ROOT);
        String currentStatus = current.status().toUpperCase(Locale.ROOT);

        if (currentStatus.equals(next)) {
            return current;
        }

        boolean allowed = false;
        if (currentStatus.equals("OPEN")) {
            allowed = next.equals("ACKNOWLEDGED") || next.equals("DISMISSED");
        } else if (currentStatus.equals("ACKNOWLEDGED")) {
            allowed = next.equals("INVESTIGATING") || next.equals("DISMISSED");
        } else if (currentStatus.equals("INVESTIGATING")) {
            allowed = next.equals("CLOSED") || next.equals("DISMISSED");
        }

        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid status transition from " + currentStatus + " to " + next);
        }

        FraudAlert updated = current.withStatus(next, notes);
        repository.updateAlert(updated);
        repository.saveAlertHistory(id, next, notes);
        return updated;
    }

    public List<AlertHistoryItem> getAlertHistory(int alertId) {
        getAlert(alertId); // Check existence
        return repository.findAlertHistory(alertId);
    }

    public DashboardSummary getSummary() {
        List<Transaction> transactions = getTransactions();
        BigDecimal volume = transactions.stream()
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long flagged = transactions.stream().filter(item -> item.status().equals("FLAGGED")).count();
        long open = getAlerts().stream()
                .filter(item -> item.status().equals("OPEN") || item.status().equals("ACKNOWLEDGED") || item.status().equals("INVESTIGATING"))
                .count();
        return new DashboardSummary(transactions.size(), (int) flagged, (int) open, volume);
    }

    // Rules CRUD Methods
    public List<Rule> getRules() {
        return repository.findAllRules();
    }

    public Rule getRule(int id) {
        return repository.findRule(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rule not found"));
    }

    public Rule createRule(Rule rule) {
        return repository.saveRule(rule);
    }

    public Rule updateRule(int id, Rule rule) {
        getRule(id); // Check existence
        Rule toUpdate = new Rule(id, rule.name(), rule.type(), rule.severity(), rule.parameters(), rule.isActive());
        return repository.updateRule(toUpdate);
    }

    public void deleteRule(int id) {
        getRule(id); // Check existence
        repository.deleteRule(id);
    }

    public Rule toggleRule(int id) {
        Rule rule = getRule(id);
        Rule toggled = new Rule(rule.id(), rule.name(), rule.type(), rule.severity(), rule.parameters(), !rule.isActive());
        return repository.updateRule(toggled);
    }

    public Map<String, Integer> seedDemo() {
        repository.clearActivity();
        List<TransactionRequest> samples = List.of(
                new TransactionRequest(1, new BigDecimal("850.00"), "PAYEE-101", "India", LocalDateTime.now().minusHours(5), "Utility payment"),
                new TransactionRequest(2, new BigDecimal("14500.00"), "PAYEE-202", "India", LocalDateTime.now().minusHours(3), "Vendor transfer"),
                new TransactionRequest(3, new BigDecimal("2200.00"), "PAYEE-303", "Germany", LocalDateTime.now().minusHours(2), "Online purchase"),
                new TransactionRequest(4, new BigDecimal("18750.00"), "PAYEE-404", "Singapore", LocalDateTime.now().withHour(2), "International transfer"),
                new TransactionRequest(1, new BigDecimal("120.00"), "PAYEE-101", "India", LocalDateTime.now().minusMinutes(20), "Card payment")
        );
        samples.forEach(this::processTransaction);
        return Map.of("transactions", getTransactions().size(), "alerts", getAlerts().size());
    }

    private String severity(int riskScore) {
        if (riskScore >= 60) return "HIGH";
        if (riskScore >= 30) return "MEDIUM";
        return "LOW";
    }
}
