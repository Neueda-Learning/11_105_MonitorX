package com.MonitorX.Services;

import com.MonitorX.Repository.MonitoringRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService} covering login/logout/session business scenarios.
 * {@link MonitoringRepository} is mocked with Mockito; password hashes are precomputed
 * with the same SHA-256 algorithm the service uses internally so we never need reflection
 * into the private hashing method.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MonitoringRepository repository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(repository);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("login succeeds and returns a token when the password hash matches")
    void login_correctPassword_returnsToken() {
        when(repository.getOperatorPasswordHash("admin")).thenReturn(Optional.of(sha256("secret123")));

        Optional<String> token = authService.login("admin", "secret123");

        assertThat(token).isPresent();
        assertThat(authService.isValidSession(token.get())).isTrue();
        assertThat(authService.getUsernameForToken(token.get())).contains("admin");
    }

    @Test
    @DisplayName("login fails when the password hash does not match")
    void login_wrongPassword_returnsEmpty() {
        when(repository.getOperatorPasswordHash("admin")).thenReturn(Optional.of(sha256("secret123")));

        Optional<String> token = authService.login("admin", "wrong-password");

        assertThat(token).isEmpty();
    }

    @Test
    @DisplayName("login fails when the username does not exist")
    void login_unknownUsername_returnsEmpty() {
        when(repository.getOperatorPasswordHash("ghost")).thenReturn(Optional.empty());

        Optional<String> token = authService.login("ghost", "whatever");

        assertThat(token).isEmpty();
    }

    @Test
    @DisplayName("successive logins for the same user produce distinct session tokens")
    void login_multipleTimes_producesDistinctTokens() {
        when(repository.getOperatorPasswordHash("admin")).thenReturn(Optional.of(sha256("secret123")));

        Optional<String> first = authService.login("admin", "secret123");
        Optional<String> second = authService.login("admin", "secret123");

        assertThat(first).isPresent();
        assertThat(second).isPresent();
        assertThat(first.get()).isNotEqualTo(second.get());
        assertThat(authService.isValidSession(first.get())).isTrue();
        assertThat(authService.isValidSession(second.get())).isTrue();
    }

    @Test
    @DisplayName("logout invalidates the session token")
    void logout_invalidatesSession() {
        when(repository.getOperatorPasswordHash("admin")).thenReturn(Optional.of(sha256("secret123")));
        String token = authService.login("admin", "secret123").orElseThrow();

        authService.logout(token);

        assertThat(authService.isValidSession(token)).isFalse();
        assertThat(authService.getUsernameForToken(token)).isEmpty();
    }

    @Test
    @DisplayName("logout with an unknown token is a safe no-op")
    void logout_unknownToken_isNoOp() {
        authService.logout("non-existent-token");

        assertThat(authService.isValidSession("non-existent-token")).isFalse();
    }

    @Test
    @DisplayName("isValidSession returns false for a token that was never issued")
    void isValidSession_unknownToken_returnsFalse() {
        assertThat(authService.isValidSession("random-token")).isFalse();
    }

    @Test
    @DisplayName("getUsernameForToken returns empty for a token that was never issued")
    void getUsernameForToken_unknownToken_returnsEmpty() {
        assertThat(authService.getUsernameForToken("random-token")).isEmpty();
    }
}
