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

@Repository
public class MonitoringRepository {
    private final Map<Integer, Customer> customers = new ConcurrentHashMap<>();
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
}