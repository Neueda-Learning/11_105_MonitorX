package com.MonitorX.Controllers;

import com.MonitorX.Services.FraudDetectionService;
import com.MonitorX.models.Customer;
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
 * Unit tests for {@link CustomerController}. {@link FraudDetectionService} is mocked with
 * Mockito; the controller is instantiated directly to verify each CRUD endpoint delegates
 * correctly.
 */
@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @Mock
    private FraudDetectionService service;

    private CustomerController controller;

    @BeforeEach
    void setUp() {
        controller = new CustomerController(service);
    }

    @Test
    @DisplayName("getAll delegates to service.getCustomers()")
    void getAll_delegatesToService() {
        Customer customer = new Customer(1, "Alice", "ACC-1", "USA");
        when(service.getCustomers()).thenReturn(List.of(customer));

        List<Customer> result = controller.getAll();

        assertThat(result).containsExactly(customer);
    }

    @Test
    @DisplayName("getById delegates to service.getCustomer(id)")
    void getById_delegatesToService() {
        Customer customer = new Customer(1, "Alice", "ACC-1", "USA");
        when(service.getCustomer(1)).thenReturn(customer);

        Customer result = controller.getById(1);

        assertThat(result).isEqualTo(customer);
    }

    @Test
    @DisplayName("create delegates to service.createCustomer(customer)")
    void create_delegatesToService() {
        Customer input = new Customer(0, "Bob", "ACC-2", "India");
        Customer created = new Customer(2, "Bob", "ACC-2", "India");
        when(service.createCustomer(input)).thenReturn(created);

        Customer result = controller.create(input);

        assertThat(result).isEqualTo(created);
    }

    @Test
    @DisplayName("update delegates to service.updateCustomer(id, customer)")
    void update_delegatesToService() {
        Customer input = new Customer(0, "Carol", "ACC-3", "UK");
        Customer updated = new Customer(3, "Carol", "ACC-3", "UK");
        when(service.updateCustomer(3, input)).thenReturn(updated);

        Customer result = controller.update(3, input);

        assertThat(result).isEqualTo(updated);
    }

    @Test
    @DisplayName("delete delegates to service.deleteCustomer(id)")
    void delete_delegatesToService() {
        controller.delete(4);

        verify(service).deleteCustomer(4);
    }
}
