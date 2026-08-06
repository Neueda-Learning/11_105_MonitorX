package com.MonitorX.models;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the bean-validation constraints declared on {@link Rule}
 * (used by {@code RuleController.create/update} via {@code @Valid}) plus its
 * plain-record accessor behavior.
 */
class RuleTest {

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

    @Test
    @DisplayName("A fully populated rule has no constraint violations")
    void validRule_hasNoViolations() {
        Rule rule = new Rule(1, "Big Amount", "AMOUNT_THRESHOLD", "HIGH", "{\"threshold\":1000}", true);

        Set<ConstraintViolation<Rule>> violations = validator.validate(rule);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Blank name is rejected")
    void blankName_isRejected() {
        Rule rule = new Rule(1, " ", "AMOUNT_THRESHOLD", "HIGH", "{}", true);

        assertThat(validator.validate(rule)).isNotEmpty();
    }

    @Test
    @DisplayName("Blank type is rejected")
    void blankType_isRejected() {
        Rule rule = new Rule(1, "Big Amount", "", "HIGH", "{}", true);

        assertThat(validator.validate(rule)).isNotEmpty();
    }

    @Test
    @DisplayName("Blank severity is rejected")
    void blankSeverity_isRejected() {
        Rule rule = new Rule(1, "Big Amount", "AMOUNT_THRESHOLD", "", "{}", true);

        assertThat(validator.validate(rule)).isNotEmpty();
    }

    @Test
    @DisplayName("Null parameters is rejected")
    void nullParameters_isRejected() {
        Rule rule = new Rule(1, "Big Amount", "AMOUNT_THRESHOLD", "HIGH", null, true);

        assertThat(validator.validate(rule)).isNotEmpty();
    }

    @Test
    @DisplayName("Accessors expose the constructor values, including the isActive flag")
    void accessors_exposeConstructorValues() {
        Rule rule = new Rule(5, "Velocity", "VELOCITY", "MEDIUM", "{}", false);

        assertThat(rule.id()).isEqualTo(5);
        assertThat(rule.name()).isEqualTo("Velocity");
        assertThat(rule.type()).isEqualTo("VELOCITY");
        assertThat(rule.severity()).isEqualTo("MEDIUM");
        assertThat(rule.parameters()).isEqualTo("{}");
        assertThat(rule.isActive()).isFalse();
    }

    @Test
    @DisplayName("Two rules with identical field values are equal")
    void equalRules_areEqual() {
        Rule a = new Rule(1, "Big Amount", "AMOUNT_THRESHOLD", "HIGH", "{}", true);
        Rule b = new Rule(1, "Big Amount", "AMOUNT_THRESHOLD", "HIGH", "{}", true);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
