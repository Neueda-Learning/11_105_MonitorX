package com.MonitorX.models;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the bean-validation constraints declared on {@link TransactionRequest}
 * (used by {@code TransactionController.create} via {@code @Valid}).
 */
class TransactionRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    private TransactionRequest validRequest() {
        return new TransactionRequest(1, BigDecimal.valueOf(100), "PAYEE-1", "USA",
                LocalDateTime.now(), "desc");
    }

    @Test
    @DisplayName("A fully populated request has no constraint violations")
    void validRequest_hasNoViolations() {
        Set<ConstraintViolation<TransactionRequest>> violations = validator.validate(validRequest());

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Null customerId is rejected")
    void nullCustomerId_isRejected() {
        TransactionRequest request = new TransactionRequest(null, BigDecimal.TEN, "P", "USA", LocalDateTime.now(), "d");

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    @DisplayName("Non-positive customerId is rejected")
    void nonPositiveCustomerId_isRejected() {
        TransactionRequest request = new TransactionRequest(0, BigDecimal.TEN, "P", "USA", LocalDateTime.now(), "d");

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    @DisplayName("Null amount is rejected")
    void nullAmount_isRejected() {
        TransactionRequest request = new TransactionRequest(1, null, "P", "USA", LocalDateTime.now(), "d");

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    @DisplayName("Amount below the 0.01 minimum is rejected")
    void amountBelowMinimum_isRejected() {
        TransactionRequest request = new TransactionRequest(1, BigDecimal.valueOf(0.001), "P", "USA", LocalDateTime.now(), "d");

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    @DisplayName("Amount exactly at the 0.01 minimum is accepted")
    void amountAtMinimum_isAccepted() {
        TransactionRequest request = new TransactionRequest(1, BigDecimal.valueOf(0.01), "P", "USA", LocalDateTime.now(), "d");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("Blank payeeId is rejected")
    void blankPayeeId_isRejected() {
        TransactionRequest request = new TransactionRequest(1, BigDecimal.TEN, "  ", "USA", LocalDateTime.now(), "d");

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    @DisplayName("payeeId longer than 60 characters is rejected")
    void tooLongPayeeId_isRejected() {
        TransactionRequest request = new TransactionRequest(1, BigDecimal.TEN, "P".repeat(61), "USA", LocalDateTime.now(), "d");

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    @DisplayName("Blank transactionCountry is rejected")
    void blankCountry_isRejected() {
        TransactionRequest request = new TransactionRequest(1, BigDecimal.TEN, "P", " ", LocalDateTime.now(), "d");

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    @DisplayName("description longer than 160 characters is rejected")
    void tooLongDescription_isRejected() {
        TransactionRequest request = new TransactionRequest(1, BigDecimal.TEN, "P", "USA", LocalDateTime.now(), "d".repeat(161));

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    @DisplayName("A null description is allowed (optional field)")
    void nullDescription_isAccepted() {
        TransactionRequest request = new TransactionRequest(1, BigDecimal.TEN, "P", "USA", LocalDateTime.now(), null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("A null timestamp is allowed (service defaults it to now())")
    void nullTimestamp_isAccepted() {
        TransactionRequest request = new TransactionRequest(1, BigDecimal.TEN, "P", "USA", null, "d");

        assertThat(validator.validate(request)).isEmpty();
    }
}
