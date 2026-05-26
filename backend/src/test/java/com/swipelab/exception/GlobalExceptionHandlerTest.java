package com.swipelab.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link GlobalExceptionHandler} stamps the correct machine-readable
 * {@code errorCode} on 403 responses so the frontend can distinguish
 * "ACCOUNT_BANNED" from "ACCESS_DENIED" without parsing the human-readable message.
 *
 * Happy flow  — UserBannedException → errorCode = "ACCOUNT_BANNED"
 * Edge case   — AccessDeniedException → errorCode = "ACCESS_DENIED"
 * Regression  — unrelated exceptions keep errorCode null
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // ── Happy flow: UserBannedException → ACCOUNT_BANNED ─────────────────────

    @Test
    @DisplayName("UserBannedException returns 403 with errorCode=ACCOUNT_BANNED")
    void userBannedException_returns403_withAccountBannedErrorCode() {
        UserBannedException ex = new UserBannedException("Account banned due to suspicious activity");

        var response = handler.handleUserBannedException(ex, new MockHttpServletRequest());

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("ACCOUNT_BANNED");
        assertThat(response.getBody().getMessage()).isEqualTo("Account banned due to suspicious activity");
    }

    // ── Edge case: AccessDeniedException → ACCESS_DENIED ─────────────────────

    @Test
    @DisplayName("AccessDeniedException returns 403 with errorCode=ACCESS_DENIED")
    void accessDeniedException_returns403_withAccessDeniedErrorCode() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");

        var response = handler.handleAccessDeniedException(ex, new MockHttpServletRequest());

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("ACCESS_DENIED");
    }

    // ── Regression: unrelated errors keep errorCode null ─────────────────────

    @Test
    @DisplayName("ResourceNotFoundException returns 404 with null errorCode")
    void resourceNotFoundException_returns404_withNullErrorCode() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found");

        var response = handler.handleResourceNotFoundException(ex, new MockHttpServletRequest());

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isNull();
    }
}
