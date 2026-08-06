package com.MonitorX.Controllers;

import com.MonitorX.Services.FraudDetectionService;
import com.MonitorX.models.AuditLogEntry;
import com.MonitorX.models.DashboardSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DashboardController}. {@link FraudDetectionService} is mocked with
 * Mockito; the controller is instantiated directly to verify summary/demo/activity endpoints
 * delegate correctly and aggregate response fields as expected.
 */
@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private FraudDetectionService service;

    private DashboardController controller;

    @BeforeEach
    void setUp() {
        controller = new DashboardController(service);
    }

    @Test
    @DisplayName("summary delegates to service.getSummary()")
    void summary_delegatesToService() {
        DashboardSummary summary = new DashboardSummary(100, 5, 2, BigDecimal.valueOf(50000));
        when(service.getSummary()).thenReturn(summary);

        DashboardSummary result = controller.summary();

        assertThat(result).isEqualTo(summary);
    }

    @Test
    @DisplayName("demo delegates the force flag to service.seedDemo(force)")
    void demo_delegatesForceFlag() {
        when(service.seedDemo(true)).thenReturn(Map.of("skipped", false));

        Map<String, Object> result = controller.demo(true);

        assertThat(result).containsEntry("skipped", false);
        verify(service).seedDemo(true);
    }

    @Test
    @DisplayName("demoStatus reports transaction/alert counts and hasData flag")
    void demoStatus_reportsCounts() {
        com.MonitorX.models.Transaction transaction = new com.MonitorX.models.Transaction(1, 1, "Alice",
                BigDecimal.TEN, "P", "USA", LocalDateTime.now(), "d", "SUCCESS", 0, List.of());
        when(service.getTransactions()).thenReturn(List.of(transaction));
        when(service.getAlerts()).thenReturn(List.of());

        Map<String, Object> result = controller.demoStatus();

        assertThat(result).containsEntry("hasData", true).containsEntry("transactions", 1).containsEntry("alerts", 0);
    }

    @Test
    @DisplayName("demoStatus reports hasData=false when there are no transactions")
    void demoStatus_noTransactions_reportsHasDataFalse() {
        when(service.getTransactions()).thenReturn(List.of());
        when(service.getAlerts()).thenReturn(List.of());

        Map<String, Object> result = controller.demoStatus();

        assertThat(result).containsEntry("hasData", false);
    }

    @Test
    @DisplayName("activity delegates to service.getRecentActivity()")
    void activity_delegatesToService() {
        AuditLogEntry entry = new AuditLogEntry(1, 1, 100, "Alice", "HIGH", "OPEN", "notes", LocalDateTime.now());
        when(service.getRecentActivity()).thenReturn(List.of(entry));

        List<AuditLogEntry> result = controller.activity();

        assertThat(result).containsExactly(entry);
    }
}
