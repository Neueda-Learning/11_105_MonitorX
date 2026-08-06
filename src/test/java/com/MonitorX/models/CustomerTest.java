package com.MonitorX.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic accessor/equality tests for the {@link Customer} record.
 */
class CustomerTest {

    @Test
    @DisplayName("Accessors expose the constructor values")
    void accessors_exposeConstructorValues() {
        Customer customer = new Customer(1, "Alice", "ACC-001", "USA");

        assertThat(customer.id()).isEqualTo(1);
        assertThat(customer.name()).isEqualTo("Alice");
        assertThat(customer.accountNumber()).isEqualTo("ACC-001");
        assertThat(customer.registeredCountry()).isEqualTo("USA");
    }

    @Test
    @DisplayName("Customers with identical field values are equal")
    void equalCustomers_areEqual() {
        Customer a = new Customer(1, "Alice", "ACC-001", "USA");
        Customer b = new Customer(1, "Alice", "ACC-001", "USA");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("Customers with different ids are not equal")
    void differentIds_areNotEqual() {
        Customer a = new Customer(1, "Alice", "ACC-001", "USA");
        Customer b = new Customer(2, "Alice", "ACC-001", "USA");

        assertThat(a).isNotEqualTo(b);
    }
}
