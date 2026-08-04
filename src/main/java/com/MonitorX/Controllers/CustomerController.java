package com.MonitorX.Controllers;

import com.MonitorX.Services.FraudDetectionService;
import com.MonitorX.models.Customer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final FraudDetectionService service;

    public CustomerController(FraudDetectionService service) {
        this.service = service;
    }

    @GetMapping
    public List<Customer> getAll() {
        return service.getCustomers();
    }
}
