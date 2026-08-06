package com.MonitorX.Controllers;

import com.MonitorX.Services.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthController}. {@link AuthService} is mocked with Mockito; the
 * controller is instantiated directly to verify request parsing, delegation, and the
 * various success/error HTTP responses for login, the OAuth2 token endpoint, logout and
 * session verification.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authService);
    }

    @Test
    @DisplayName("login with valid credentials returns 200 with a token and username")
    void login_validCredentials_returnsOkWithToken() {
        when(authService.login("admin", "secret")).thenReturn(Optional.of("token-123"));
        Map<String, String> credentials = Map.of("username", "admin", "password", "secret");

        ResponseEntity<Map<String, String>> response = controller.login(credentials);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("token", "token-123").containsEntry("username", "admin");
    }

    @Test
    @DisplayName("login with invalid credentials returns 401")
    void login_invalidCredentials_returnsUnauthorized() {
        when(authService.login("admin", "wrong")).thenReturn(Optional.empty());
        Map<String, String> credentials = Map.of("username", "admin", "password", "wrong");

        ResponseEntity<Map<String, String>> response = controller.login(credentials);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsKey("error");
    }

    @Test
    @DisplayName("login with a missing username returns 400 without calling the service")
    void login_missingUsername_returnsBadRequest() {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("password", "secret");

        ResponseEntity<Map<String, String>> response = controller.login(credentials);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(authService, never()).login(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("login with a missing password returns 400 without calling the service")
    void login_missingPassword_returnsBadRequest() {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", "admin");

        ResponseEntity<Map<String, String>> response = controller.login(credentials);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(authService, never()).login(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("OAuth2 token endpoint returns an access_token on success")
    void token_validCredentials_returnsAccessToken() {
        when(authService.login("admin", "secret")).thenReturn(Optional.of("token-456"));

        ResponseEntity<Map<String, String>> response = controller.token("admin", "secret");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("access_token", "token-456").containsEntry("token_type", "bearer");
    }

    @Test
    @DisplayName("OAuth2 token endpoint returns 401 on invalid credentials")
    void token_invalidCredentials_returnsUnauthorized() {
        when(authService.login("admin", "wrong")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, String>> response = controller.token("admin", "wrong");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("logout with a Bearer header extracts the token and delegates to the service")
    void logout_withBearerHeader_delegatesToService() {
        controller.logout("Bearer abc-123");

        verify(authService).logout("abc-123");
    }

    @Test
    @DisplayName("logout without an Authorization header does not call the service")
    void logout_withoutHeader_doesNothing() {
        controller.logout(null);

        verify(authService, never()).logout(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("verify returns valid=true with username for an active session")
    void verify_validSession_returnsValidTrue() {
        when(authService.isValidSession("abc-123")).thenReturn(true);
        when(authService.getUsernameForToken("abc-123")).thenReturn(Optional.of("admin"));

        ResponseEntity<Map<String, Object>> response = controller.verify("Bearer abc-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("valid", true).containsEntry("username", "admin");
    }

    @Test
    @DisplayName("verify returns 401 for an invalid or expired session")
    void verify_invalidSession_returnsUnauthorized() {
        when(authService.isValidSession("expired-token")).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = controller.verify("Bearer expired-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("verify returns 401 when no Authorization header is present")
    void verify_missingHeader_returnsUnauthorized() {
        ResponseEntity<Map<String, Object>> response = controller.verify(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
