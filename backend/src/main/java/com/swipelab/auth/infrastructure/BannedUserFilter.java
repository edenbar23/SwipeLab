package com.swipelab.auth.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Intercepts every authenticated request and short-circuits with a
 * {@code 403 ACCOUNT_BANNED} response if the user's account is banned.
 *
 * <p>Placement: runs <em>after</em> {@link JwtAuthenticationFilter} and
 * {@link com.swipelab.auth.external.ExternalAuthFilter} so the SecurityContext
 * is already populated when this filter executes.
 *
 * <p>Skips: unauthenticated requests, public auth paths, and OPTIONS preflight.
 *
 * <p>The JSON body mirrors {@link com.swipelab.dto.response.ErrorResponse} so
 * the frontend's global {@code apiFetch} interceptor can detect
 * {@code errorCode == "ACCOUNT_BANNED"} uniformly for all endpoints.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BannedUserFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Skip unauthenticated requests — let the security layer handle those
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = auth.getName();

        // Re-query to get the live DB state (avoids stale cached UserDetails)
        User user = userRepository.findByUsername(username).orElse(null);

        if (user != null && isBanned(user)) {
            log.warn("Blocked banned user '{}' from accessing {}", username, request.getServletPath());
            writeBannedResponse(request, response, username);
            return; // Halt the filter chain — do NOT call filterChain.doFilter
        }

        filterChain.doFilter(request, response);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * A user is considered banned if either the status field is BANNED
     * or accountLocked is explicitly true (set by both auto-ban and manual ban).
     */
    private boolean isBanned(User user) {
        return com.swipelab.model.enums.UserStatus.BANNED.equals(user.getStatus())
                || Boolean.TRUE.equals(user.getAccountLocked());
    }

    private void writeBannedResponse(HttpServletRequest request,
                                     HttpServletResponse response,
                                     String username) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", 403);
        body.put("error", "Forbidden");
        body.put("message", "Account banned due to suspicious activity");
        body.put("path", request.getServletPath());
        body.put("errorCode", "ACCOUNT_BANNED");

        objectMapper.writeValue(response.getWriter(), body);
    }

    /**
     * Skip the ban check for public paths that don't require authentication.
     * Auth endpoints must remain accessible so a banned user can still call /login
     * and receive the proper error from {@link com.swipelab.auth.application.AuthenticationService}.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        if (path == null) return false;
        return path.startsWith("/api/v1/auth/")
                || path.startsWith("/auth/")
                || path.startsWith("/oauth2/")
                || path.startsWith("/login/")
                || path.startsWith("/swipe_lab/")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui");
    }
}
