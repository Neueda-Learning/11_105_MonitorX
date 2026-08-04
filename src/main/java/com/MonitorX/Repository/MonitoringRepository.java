  package com.MonitorX.Repository;

import com.MonitorX.models.Customer;
import com.MonitorX.models.FraudAlert;
import com.MonitorX.models.Transaction;
import com.MonitorX.models.Rule;
import com.MonitorX.models.AlertHistoryItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
public class MonitoringRepository {
    private final JdbcTemplate jdbc;

    public MonitoringRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
// Customer Methods
    public List<Customer> findAllCustomers() {
        return jdbc.query("SELECT id, name, account_number, registered_country FROM customers ORDER BY id", this::mapCustomer);
    }

    public Optional<Customer> findCustomer(int id) {
        List<Customer> results = jdbc.query("SELECT id, name, account_number, registered_country FROM customers WHERE id = ?", this::mapCustomer, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Customer saveCustomer(Customer customer) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO customers (name, account_number, registered_country) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, customer.name());
            ps.setString(2, customer.accountNumber());
            ps.setString(3, customer.registeredCountry());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        int id = key != null ? key.intValue() : 0;
        return new Customer(id, customer.name(), customer.accountNumber(), customer.registeredCountry());
    }

    public Customer updateCustomer(Customer customer) {
        jdbc.update(
                "UPDATE customers SET name = ?, account_number = ?, registered_country = ? WHERE id = ?",
                customer.name(),
                customer.accountNumber(),
                customer.registeredCountry(),
                customer.id()
        );
        return customer;
    }

    public void deleteCustomer(int id) {
        jdbc.update("DELETE FROM customers WHERE id = ?", id);
    }

    // Operator Methods
    public Optional<String> getOperatorPasswordHash(String username) {
        List<String> results = jdbc.query(
                "SELECT password_hash FROM operators WHERE username = ?",
                (rs, rowNum) -> rs.getString("password_hash"),
                username
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
