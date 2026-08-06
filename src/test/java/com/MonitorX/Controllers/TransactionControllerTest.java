package com.MonitorX.Controllers;

import com.MonitorX.Services.FraudDetectionService;
import com.MonitorX.models.Transaction;
import com.MonitorX.models.TransactionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TransactionController}. {@link FraudDetectionService} is mocked
 * with Mockito; the controller is instantiated directly to verify each endpoint delegates
 * correctly, including the fraud-evaluation entry point ({@code processTransaction}).
 */
@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private FraudDetectionService service;

    private TransactionController controller;

    @BeforeEach
    void setUp() {
        controller = new TransactionController(service);
    }

    @Test
    @DisplayName("getAll delegates to service.getTransactions()")
    void getAll_delegatesToService() {
        Transaction transaction = new Transaction(1, 1, "Alice", BigDecimal.TEN, "P", "USA",
                LocalDateTime.now(), "d", "SUCCESS", 0, List.of());
        when(service.getTransactions()).thenReturn(List.of(transaction));

        List<Transaction> result = controller.getAll();

        assertThat(result).containsExactly(transaction);
    }

    @Test
    @DisplayName("getById delegates to service.getTransaction(id)")
    void getById_delegatesToService() {
        Transaction transaction = new Transaction(1, 1, "Alice", BigDecimal.TEN, "P", "USA",
                LocalDateTime.now(), "d", "SUCCESS", 0, List.of());
        when(service.getTransaction(1)).thenReturn(transaction);

        Transaction result = controller.getById(1);

        assertThat(result).isEqualTo(transaction);
    }

    @Test
    @DisplayName("create delegates the request to service.processTransaction(request)")
    void create_delegatesToFraudEvaluation() {
        TransactionRequest request = new TransactionRequest(1, BigDecimal.valueOf(5000), "PAYEE-1", "UK",
                LocalDateTime.now(), "desc");
        Transaction flagged = new Transaction(1, 1, "Alice", BigDecimal.valueOf(5000), "PAYEE-1", "UK",
                LocalDateTime.now(), "desc", "FLAGGED", 30, List.of("Transaction country differs from customer country"));
        when(service.processTransaction(request)).thenReturn(flagged);

        Transaction result = controller.create(request);

        assertThat(result.status()).isEqualTo("FLAGGED");
        verify(service).processTransaction(request);
    }

    @Test
    @DisplayName("delete delegates to service.deleteTransaction(id)")
    void delete_delegatesToService() {
        controller.delete(9);

        verify(service).deleteTransaction(9);
    }
}
