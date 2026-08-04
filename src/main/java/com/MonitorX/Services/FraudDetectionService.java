package com.MonitorX.Services;

import com.MonitorX.Repository.MonitoringRepository;
import com.MonitorX.models.Customer;
import com.MonitorX.models.FraudAlert;
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
}