package com.MonitorX.Services;

import com.MonitorX.Repository.MonitoringRepository;
import com.MonitorX.models.Customer;
import com.MonitorX.models.FraudAlert;
import com.MonitorX.models.Transaction;
import com.MonitorX.models.TransactionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class FraudDetectionService {
    private static final BigDecimal HIGH_AMOUNT = new BigDecimal("10000");
    private static final Set<String> ALERT_STATUSES = Set.of("OPEN", "REVIEWING", "RESOLVED", "DISMISSED");

    private final MonitoringRepository repository;

    public FraudDetectionService(MonitoringRepository repository) {
        this.repository = repository;
    }

    public List<Customer> getCustomers() {
        return repository.findAllCustomers();
    }
    public List<Transaction> getTransactions() {
        return repository.findAllTransactions();
    }
     public Transaction getTransaction(int id) {
        return repository.findTransaction(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }
    
    public List<FraudAlert> getAlerts() {
        return repository.findAllAlerts();
    }

    public FraudAlert updateAlertStatus(int id, String requestedStatus) {
        FraudAlert current = repository.findAlert(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));
        String next = requestedStatus == null ? "" : requestedStatus.trim().toUpperCase(Locale.ROOT);
        if (!ALERT_STATUSES.contains(next)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid alert status");
        }
        if ((current.status().equals("RESOLVED") || current.status().equals("DISMISSED"))
                && !current.status().equals(next)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Closed alerts cannot be reopened");
        }
        return repository.updateAlert(current.withStatus(next));
    }

}