package com.MonitorX.Controllers;

import com.MonitorX.Services.FraudDetectionService;
import com.MonitorX.models.Rule;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
public class RuleController {
    private final FraudDetectionService service;

    public RuleController(FraudDetectionService service) {
        this.service = service;
    }

    @GetMapping
    public List<Rule> getAll() {
        return service.getRules();
    }

    @GetMapping("/{id}")
    public Rule getById(@PathVariable int id) {
        return service.getRule(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Rule create(@Valid @RequestBody Rule rule) {
        return service.createRule(rule);
    }

    @PutMapping("/{id}")
    public Rule update(@PathVariable int id, @Valid @RequestBody Rule rule) {
        return service.updateRule(id, rule);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable int id) {
        service.deleteRule(id);
    }

    @PatchMapping("/{id}/toggle")
    public Rule toggle(@PathVariable int id) {
        return service.toggleRule(id);
    }
}
