package com.MonitorX.Controllers;

import com.MonitorX.Services.FraudDetectionService;
import com.MonitorX.models.FraudAlert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {
    private final FraudDetectionService service;

    public AlertController(FraudDetectionService service) {
        this.service = service;
    }

    @GetMapping
    public List<FraudAlert> getAll() {
        return service.getAlerts();
    }

    @PatchMapping("/{id}/status")
    public FraudAlert updateStatus(@PathVariable int id, @RequestBody Map<String, String> body) {
        return service.updateAlertStatus(id, body.get("status"));
    }
}
