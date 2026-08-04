package com.MonitorX.Controllers;

import com.MonitorX.Services.FraudDetectionService;
import com.MonitorX.models.Customer;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{id}")
    public Customer getById(@PathVariable int id) {
        return service.getCustomer(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Customer create(@RequestBody Customer customer) {
        return service.createCustomer(customer);
    }

    @PutMapping("/{id}")
    public Customer update(@PathVariable int id, @RequestBody Customer customer) {
        return service.updateCustomer(id, customer);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable int id) {
        service.deleteCustomer(id);
    }
}
