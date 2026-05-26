package com.swipelab.auth.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swipelab.model.enums.UserStatus;
import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BannedUserFilter}.
 *
 * Happy flow  — active user passes through, filter chain continues
 * Edge case 1 — banned user (status=BANNED) receives 403 ACCOUNT_BANNED
 * Edge case 2 — banned user (accountLocked=true) receives 403 ACCOUNT_BANNED
 * Edge case 3 — unauthenticated request passes through without a DB lookup
 */
@ExtendWith(MockitoExtension.class)
class BannedUserFilterTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private BannedUserFilter bannedUserFilter;

    // Use a real ObjectMapper — it has no external dependencies
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        // Inject the real ObjectMapper via the field (InjectMocks only covers constructor injection)
        var field = org.springframework.test.util.ReflectionTestUtils.class;
        org.springframework.test.util.ReflectionTestUtils.setField(bannedUserFilter, "objectMapper", objectMapper);
    }

    // ── Happy flow: active user passes through ────────────────────────────────

    @Test
    @DisplayName("Active user: filter chain continues, no 403 written")
    void activeUser_passesThrough() throws Exception {
        authenticateAs("alice");

        User activeUser = buildUser("alice", UserStatus.ACTIVE, false);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(activeUser));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        bannedUserFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(HttpServletResponse.SC_FORBIDDEN);
    }

    // ── Edge case 1: status=BANNED → 403 ─────────────────────────────────────

    @Test
    @DisplayName("User with status=BANNED receives 403 ACCOUNT_BANNED")
    void bannedStatus_returns403_withAccountBannedCode() throws Exception {
        authenticateAs("mallory");

        User bannedUser = buildUser("mallory", UserStatus.BANNED, false);
        when(userRepository.findByUsername("mallory")).thenReturn(Optional.of(bannedUser));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        bannedUserFilter.doFilterInternal(request, response, filterChain);

        // Filter chain must NOT continue
        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("ACCOUNT_BANNED");
    }

    // ── Edge case 2: accountLocked=true → 403 ────────────────────────────────

    @Test
    @DisplayName("User with accountLocked=true receives 403 ACCOUNT_BANNED")
    void accountLocked_returns403_withAccountBannedCode() throws Exception {
        authenticateAs("locked_user");

        User lockedUser = buildUser("locked_user", UserStatus.ACTIVE, true);
        when(userRepository.findByUsername("locked_user")).thenReturn(Optional.of(lockedUser));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        bannedUserFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("ACCOUNT_BANNED");
    }

    // ── Edge case 3: unauthenticated request passes without DB lookup ─────────

    @Test
    @DisplayName("Unauthenticated request: no DB lookup, filter chain continues")
    void unauthenticatedRequest_passesThrough_withoutDbLookup() throws Exception {
        // No authentication set in SecurityContext
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        bannedUserFilter.doFilterInternal(request, response, filterChain);

        verify(userRepository, never()).findByUsername(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void authenticateAs(String username) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private User buildUser(String username, UserStatus status, boolean accountLocked) {
        User user = new User();
        user.setUsername(username);
        user.setStatus(status);
        user.setAccountLocked(accountLocked);
        user.setActive(!accountLocked && status == UserStatus.ACTIVE);
        return user;
    }
}
