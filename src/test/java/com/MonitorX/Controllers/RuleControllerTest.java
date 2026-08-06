package com.MonitorX.Controllers;

import com.MonitorX.Services.FraudDetectionService;
import com.MonitorX.models.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RuleController}. {@link FraudDetectionService} is mocked with
 * Mockito; the controller is instantiated directly to verify each rules-engine management
 * endpoint (CRUD + toggle) delegates correctly.
 */
@ExtendWith(MockitoExtension.class)
class RuleControllerTest {

    @Mock
    private FraudDetectionService service;

    private RuleController controller;

    @BeforeEach
    void setUp() {
        controller = new RuleController(service);
    }

    @Test
    @DisplayName("getAll delegates to service.getRules()")
    void getAll_delegatesToService() {
        Rule rule = new Rule(1, "Big Amount", "AMOUNT_THRESHOLD", "HIGH", "{\"threshold\":1000}", true);
        when(service.getRules()).thenReturn(List.of(rule));

        List<Rule> result = controller.getAll();

        assertThat(result).containsExactly(rule);
    }

    @Test
    @DisplayName("getById delegates to service.getRule(id)")
    void getById_delegatesToService() {
        Rule rule = new Rule(1, "Big Amount", "AMOUNT_THRESHOLD", "HIGH", "{\"threshold\":1000}", true);
        when(service.getRule(1)).thenReturn(rule);

        Rule result = controller.getById(1);

        assertThat(result).isEqualTo(rule);
    }

    @Test
    @DisplayName("create delegates to service.createRule(rule)")
    void create_delegatesToService() {
        Rule input = new Rule(0, "Velocity", "VELOCITY", "MEDIUM", "{}", true);
        Rule created = new Rule(2, "Velocity", "VELOCITY", "MEDIUM", "{}", true);
        when(service.createRule(input)).thenReturn(created);

        Rule result = controller.create(input);

        assertThat(result).isEqualTo(created);
    }

    @Test
    @DisplayName("update delegates to service.updateRule(id, rule)")
    void update_delegatesToService() {
        Rule input = new Rule(0, "Daily Limit", "DAILY_LIMIT", "HIGH", "{\"dailyLimit\":5000}", true);
        Rule updated = new Rule(3, "Daily Limit", "DAILY_LIMIT", "HIGH", "{\"dailyLimit\":5000}", true);
        when(service.updateRule(3, input)).thenReturn(updated);

        Rule result = controller.update(3, input);

        assertThat(result).isEqualTo(updated);
    }

    @Test
    @DisplayName("delete delegates to service.deleteRule(id)")
    void delete_delegatesToService() {
        controller.delete(4);

        verify(service).deleteRule(4);
    }

    @Test
    @DisplayName("toggle delegates to service.toggleRule(id)")
    void toggle_delegatesToService() {
        Rule toggled = new Rule(5, "New Payee", "NEW_PAYEE", "LOW", "{}", false);
        when(service.toggleRule(5)).thenReturn(toggled);

        Rule result = controller.toggle(5);

        assertThat(result.isActive()).isFalse();
    }
}
