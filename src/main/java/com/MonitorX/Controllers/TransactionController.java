package com.MonitorX.Controllers;

import com.MonitorX.Services.FraudDetectionService;
import com.MonitorX.models.Transaction;
import com.MonitorX.models.TransactionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final FraudDetectionService service;

    public TransactionController(FraudDetectionService service) {
        this.service = service;
    }

    @GetMapping
    public List<Transaction> getAll() {
        return service.getTransactions();
    }

    @GetMapping("/{id}")
    public Transaction getById(@PathVariable int id) {
        return service.getTransaction(id);
    }

    
}
