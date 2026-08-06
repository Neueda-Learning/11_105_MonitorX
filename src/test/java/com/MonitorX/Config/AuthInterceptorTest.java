package com.MonitorX.Config;

import com.MonitorX.Services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.PrintWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthInterceptor}, covering the routing/authorization
 * decisions made in {@code preHandle} for protected vs. public/auth endpoints.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthInterceptorTest {

    @Mock
    private AuthService authService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private AuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new AuthInterceptor(authService);
    }

    @Test
    @DisplayName("OPTIONS requests are always allowed through, regardless of path or headers")
    void optionsRequest_alwaysAllowed() throws Exception {
        when(request.getMethod()).thenReturn("OPTIONS");
        when(request.getRequestURI()).thenReturn("/api/customers");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    @DisplayName("Requests outside /api are allowed through without a token")
    void nonApiRequest_allowedWithoutToken() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/index.html");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(authService, never()).isValidSession(any());
    }

    @Test
    @DisplayName("Requests to /api/auth endpoints are allowed through without a token")
    void authEndpoint_allowedWithoutToken() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/auth/login");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(authService, never()).isValidSession(any());
    }

    @Test
    @DisplayName("Protected endpoint with a valid Bearer token is allowed through")
    void protectedEndpoint_validToken_allowed() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/customers");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(authService.isValidSession("valid-token")).thenReturn(true);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    @DisplayName("Protected endpoint with an invalid Bearer token returns 401")
    void protectedEndpoint_invalidToken_unauthorized() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/customers");
        when(request.getHeader("Authorization")).thenReturn("Bearer bad-token");
        when(authService.isValidSession("bad-token")).thenReturn(false);
        PrintWriter writer = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(writer);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(writer).write(contains("Unauthorized"));
    }

    @Test
    @DisplayName("Protected endpoint with a missing Authorization header returns 401")
    void protectedEndpoint_missingHeader_unauthorized() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/customers");
        when(request.getHeader("Authorization")).thenReturn(null);
        PrintWriter writer = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(writer);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(authService, never()).isValidSession(any());
    }

    @Test
    @DisplayName("Protected endpoint with a non-Bearer Authorization header returns 401")
    void protectedEndpoint_nonBearerHeader_unauthorized() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/customers");
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");
        PrintWriter writer = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(writer);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(authService, never()).isValidSession(any());
    }
}
