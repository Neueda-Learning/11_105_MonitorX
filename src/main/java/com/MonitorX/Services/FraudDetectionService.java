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
}