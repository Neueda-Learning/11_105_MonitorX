package com.MonitorX.Repository;

import com.MonitorX.models.Customer;
import com.MonitorX.models.FraudAlert;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.MonitorX.models.Transaction;



import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
@Repository
public class MonitoringRepository {
    private final Map<Integer, Customer> customers = new ConcurrentHashMap<>();
    private final Map<Integer, Transaction> transactions = new ConcurrentHashMap<>();
    private final AtomicInteger transactionIds = new AtomicInteger(1);

    private final Map<Integer, FraudAlert> alerts = new ConcurrentHashMap<>();
    private final AtomicInteger alertIds = new AtomicInteger(1);

    public MonitoringRepository() {
        customers.put(1, new Customer(1, "Rahul Sharma", "ACC1001", "India"));
        customers.put(2, new Customer(2, "Priya Verma", "ACC1002", "India"));
        customers.put(3, new Customer(3, "John Smith", "ACC1003", "USA"));
        customers.put(4, new Customer(4, "Amina Yusuf", "ACC1004", "UAE"));
    }

    public List<Customer> findAllCustomers() {
        return customers.values().stream().sorted(Comparator.comparingInt(Customer::id)).toList();
    }

    public Optional<Customer> findCustomer(int id) {
        return Optional.ofNullable(customers.get(id));
    }
    public Transaction saveTransaction(Transaction transaction) {
        int id = transactionIds.getAndIncrement();
        Transaction saved = new Transaction(id, transaction.customerId(), transaction.customerName(),
                transaction.amount(), transaction.transactionCountry(), transaction.timestamp(),
                transaction.description(), transaction.status(), transaction.riskScore(), transaction.reasons());
        transactions.put(id, saved);
        return saved;
    }

    public List<Transaction> findAllTransactions() {
        return transactions.values().stream()
                .sorted(Comparator.comparing(Transaction::timestamp).reversed())
                .toList();
    }

    public Optional<Transaction> findTransaction(int id) {
        return Optional.ofNullable(transactions.get(id));
    }

    public void deleteTransaction(int id) {
        transactions.remove(id);
        alerts.entrySet().removeIf(entry -> entry.getValue().transactionId() == id);
    }
    
    public FraudAlert saveAlert(FraudAlert alert) {
        int id = alertIds.getAndIncrement();
        FraudAlert saved = new FraudAlert(id, alert.transactionId(), alert.customerName(), alert.severity(),
                alert.status(), alert.riskScore(), alert.reasons(), alert.createdAt());
        alerts.put(id, saved);
        return saved;
    }

    public List<FraudAlert> findAllAlerts() {
        return alerts.values().stream()
                .sorted(Comparator.comparing(FraudAlert::createdAt).reversed())
                .toList();
    }

    public Optional<FraudAlert> findAlert(int id) {
        return Optional.ofNullable(alerts.get(id));
    }

    public FraudAlert updateAlert(FraudAlert alert) {
        alerts.put(alert.id(), alert);
        return alert;
    }

    public void clearActivity() {
        transactions.clear();
        alerts.clear();
        transactionIds.set(1);
        alertIds.set(1);
    }
}