package com.MonitorX.Controllers;

import com.MonitorX.Services.FraudDetectionService;
import com.MonitorX.models.AlertHistoryItem;
import com.MonitorX.models.FraudAlert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AlertController}. {@link FraudDetectionService} is mocked with
 * Mockito; the controller is instantiated directly (no web layer) to verify it correctly
 * delegates each endpoint to the service and passes through parameters/results.
 */
@ExtendWith(MockitoExtension.class)
class AlertControllerTest {

    @Mock
    private FraudDetectionService service;

    private AlertController controller;

    @BeforeEach
    void setUp() {
        controller = new AlertController(service);
    }

    @Test
    @DisplayName("getAll delegates to service.getAlerts()")
    void getAll_delegatesToService() {
        FraudAlert alert = new FraudAlert(1, 10, 1, "Alice", "HIGH", "OPEN", 70,
                List.of("reason"), "", LocalDateTime.now(), LocalDateTime.now());
        when(service.getAlerts()).thenReturn(List.of(alert));

        List<FraudAlert> result = controller.getAll();

        assertThat(result).containsExactly(alert);
    }

    @Test
    @DisplayName("getById delegates to service.getAlert(id)")
    void getById_delegatesToService() {
        FraudAlert alert = new FraudAlert(1, 10, 1, "Alice", "HIGH", "OPEN", 70,
                List.of("reason"), "", LocalDateTime.now(), LocalDateTime.now());
        when(service.getAlert(1)).thenReturn(alert);

        FraudAlert result = controller.getById(1);

        assertThat(result).isEqualTo(alert);
    }

    @Test
    @DisplayName("updateStatus extracts status/notes from the request body and delegates to the service")
    void updateStatus_delegatesWithExtractedFields() {
        FraudAlert updated = new FraudAlert(1, 10, 1, "Alice", "HIGH", "ACKNOWLEDGED", 70,
                List.of("reason"), "reviewing", LocalDateTime.now(), LocalDateTime.now());
        Map<String, String> body = new HashMap<>();
        body.put("status", "ACKNOWLEDGED");
        body.put("notes", "reviewing");
        when(service.updateAlertStatus(1, "ACKNOWLEDGED", "reviewing")).thenReturn(updated);

        FraudAlert result = controller.updateStatus(1, body);

        assertThat(result.status()).isEqualTo("ACKNOWLEDGED");
        verify(service).updateAlertStatus(1, "ACKNOWLEDGED", "reviewing");
    }

    @Test
    @DisplayName("updateStatus defaults null notes to an empty string before calling the service")
    void updateStatus_nullNotes_defaultsToEmptyString() {
        Map<String, String> body = new HashMap<>();
        body.put("status", "DISMISSED");
        body.put("notes", null);

        controller.updateStatus(1, body);

        verify(service).updateAlertStatus(1, "DISMISSED", "");
    }

    @Test
    @DisplayName("getHistory delegates to service.getAlertHistory(id)")
    void getHistory_delegatesToService() {
        AlertHistoryItem historyItem = new AlertHistoryItem(1, 1, "OPEN", "created", LocalDateTime.now());
        when(service.getAlertHistory(1)).thenReturn(List.of(historyItem));

        List<AlertHistoryItem> result = controller.getHistory(1);

        assertThat(result).containsExactly(historyItem);
    }
}
