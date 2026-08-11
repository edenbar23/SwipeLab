package com.swipelab.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * Applies Cache-Control headers to responses based on request path and method.
 *
 * Rules (evaluated top-to-bottom; first match wins):
 *   - Non-GET/HEAD methods → no-store (mutations must never be cached)
 *   - Sensitive / always-fresh paths → no-store
 *   - Image content → public, max-age=86400 (immutable once uploaded)
 *   - Shared public data (leaderboard) → public, max-age=60
 *   - Per-user volatile data → private, max-age=30
 *   - Per-user standard data → private, max-age=60
 *   - Stable lookup data → private, max-age=120 or max-age=300
 *   - User profile (always revalidate) → private, no-cache
 *   - Everything else → no Cache-Control header added
 *
 * Registered in WebConfig.addInterceptors().
 */
@Component
public class CacheControlInterceptor implements HandlerInterceptor {

    // Pre-built CacheControl instances (immutable, safe to share)
    private static final String NO_STORE          = CacheControl.noStore().getHeaderValue();
    private static final String NO_CACHE_PRIVATE  = CacheControl.noCache().cachePrivate().getHeaderValue();
    private static final String PUBLIC_1DAY       = CacheControl.maxAge(86400, TimeUnit.SECONDS).cachePublic().getHeaderValue();
    private static final String PUBLIC_60S        = CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic().getHeaderValue();
    private static final String PRIVATE_30S       = CacheControl.maxAge(30, TimeUnit.SECONDS).cachePrivate().getHeaderValue();
    private static final String PRIVATE_60S       = CacheControl.maxAge(60, TimeUnit.SECONDS).cachePrivate().getHeaderValue();
    private static final String PRIVATE_120S      = CacheControl.maxAge(120, TimeUnit.SECONDS).cachePrivate().getHeaderValue();
    private static final String PRIVATE_300S      = CacheControl.maxAge(300, TimeUnit.SECONDS).cachePrivate().getHeaderValue();

    private static final String CACHE_CONTROL     = "Cache-Control";

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        String method = request.getMethod();
        String path   = request.getRequestURI();

        // ── Non-GET/HEAD → always no-store ────────────────────────────────────
        if (!HttpMethod.GET.matches(method) && !HttpMethod.HEAD.matches(method)) {
            response.setHeader(CACHE_CONTROL, NO_STORE);
            return true;
        }

        // ── Sensitive / must-not-cache ─────────────────────────────────────────
        if (path.startsWith("/api/v1/auth/")
                || path.startsWith("/health")
                || path.startsWith("/api/v1/classifications/next-batch")) {
            response.setHeader(CACHE_CONTROL, NO_STORE);
            return true;
        }

        // ── Immutable binary content (images) ─────────────────────────────────
        if (path.matches(".*/images/[^/]+/content")
                || path.matches(".*/gold-images/[^/]+/image")) {
            response.setHeader(CACHE_CONTROL, PUBLIC_1DAY);
            return true;
        }

        // ── Shared public data ─────────────────────────────────────────────────
        if (path.equals("/api/v1/gamification/leaderboard")) {
            response.setHeader(CACHE_CONTROL, PUBLIC_60S);
            return true;
        }

        // ── Per-user volatile (changes on every classification) ────────────────
        if (path.startsWith("/api/v1/gamification/user-info")
                || path.startsWith("/api/v1/gamification/rank")
                || path.startsWith("/api/v1/classifications/progress")) {
            response.setHeader(CACHE_CONTROL, PRIVATE_30S);
            return true;
        }

        // ── User profile (always revalidate via ETag) ──────────────────────────
        if (path.equals("/api/v1/users/me")) {
            response.setHeader(CACHE_CONTROL, NO_CACHE_PRIVATE);
            return true;
        }

        // ── Taxonomy / species metadata (10-min Caffeine TTL) ─────────────────
        if (path.startsWith("/api/v1/metadata/")) {
            response.setHeader(CACHE_CONTROL, PRIVATE_300S);
            return true;
        }

        // ── Task lists & details (always revalidate with backend) ────────────
        // These are volatile and mutate the moment the user self-assigns a task
        // or a researcher pauses/archives a task. We use NO_CACHE_PRIVATE so
        // the client caches them but ALWAYS revalidates via ETags, preventing
        // stale UI bugs while saving bandwidth when nothing has changed.
        if (path.equals("/api/v1/tasks/my-tasks")
                || path.equals("/api/v1/tasks/available-tasks")
                || path.equals("/api/v1/tasks/dashboard")
                || path.matches(".*/tasks/my-tasks/[^/]+")
                || path.matches(".*/tasks/dashboard/[^/]+")) {
            response.setHeader(CACHE_CONTROL, NO_CACHE_PRIVATE);
            return true;
        }

        // ── User public profile ────────────────────────────────────────────────
        if (path.matches("/api/v1/users/[^/]+")) {
            response.setHeader(CACHE_CONTROL, PRIVATE_120S);
            return true;
        }

        // ── Analytics task-level (admin) ───────────────────────────────────────
        if (path.startsWith("/api/v1/analytics/")) {
            response.setHeader(CACHE_CONTROL, PRIVATE_120S);
            return true;
        }

        // ── Standard private, 60 s ────────────────────────────────────────────
        if (path.startsWith("/api/v1/tasks/")
                || path.startsWith("/api/v1/gamification/")
                || path.startsWith("/api/v1/collection")
                || path.startsWith("/api/v1/statistics/")
                || path.startsWith("/api/v1/classifications/")) {
            response.setHeader(CACHE_CONTROL, PRIVATE_60S);
            return true;
        }

        // ── Everything else: no header added (Spring default behaviour) ────────
        return true;
    }
}
