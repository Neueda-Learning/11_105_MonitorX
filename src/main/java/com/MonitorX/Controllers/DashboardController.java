package com.MonitorX.Controllers;

import com.MonitorX.Services.FraudDetectionService;
import com.MonitorX.models.AuditLogEntry;
import com.MonitorX.models.DashboardSummary;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DashboardController {
    private final FraudDetectionService service;

    public DashboardController(FraudDetectionService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public DashboardSummary summary() {
        return service.getSummary();
    }

    @PostMapping("/demo")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> demo(@RequestParam(value = "force", defaultValue = "false") boolean force) {
        return service.seedDemo(force);
    }

    @GetMapping("/demo/status")
    public Map<String, Object> demoStatus() {
        int txCount = service.getTransactions().size();
        int alertCount = service.getAlerts().size();
        return Map.of(
            "hasData", txCount > 0,
            "transactions", txCount,
            "alerts", alertCount
        );
    }

    @GetMapping("/activity")
    public List<AuditLogEntry> activity() {
        return service.getRecentActivity();
    }
}
