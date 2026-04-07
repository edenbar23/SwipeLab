package com.swipelab.auth.external;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalAuthFilter extends OncePerRequestFilter {

    private final StardbiAuthService stardbiAuthService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Only attempt external auth if not already authenticated (e.g., by JwtAuthenticationFilter)
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String token = getJwtFromRequest(request);
                if (StringUtils.hasText(token)) {
                    // Fast check: look if token looks like a Stardbi token before doing a network call.
                    // This is handled by processStardbiToken validating it
                    UserDetails userDetails = stardbiAuthService.processStardbiToken(token);
                    if (userDetails != null) {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } else if (looksLikeStardbiToken(token)) {
                        // If it looked like a Stardbi token but validation failed, it's expired/invalid.
                        // "if expired -> try refresh; if refresh fails -> reject (401)"
                        log.warn("Stardbi token rejected or expired.");
                        // The user requirements requested rejection with 401:
                        // "reject (401)"
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"External token expired or invalid. Please refresh.\"}");
                        return; // Halt filter chain
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Could not set user authentication via external provider in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
    
    // A heuristic to avoid sending random words to the Stardbi endpoint.
    private boolean looksLikeStardbiToken(String token) {
        // Simple JWT check: contains two dots
        return token.split("\\.").length == 3;
    }
}
