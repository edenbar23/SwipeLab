package com.swipelab.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CacheControlInterceptor.
 *
 * Note: Spring's CacheControl serializes directives as:
 *   "max-age=N, private" (not "private, max-age=N")
 *   "max-age=N, public" (not "public, max-age=N")
 */
class CacheControlInterceptorTest {

    private CacheControlInterceptor interceptor;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new CacheControlInterceptor();
        response = new MockHttpServletResponse();
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private void handle(String method, String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        interceptor.preHandle(request, response, new Object());
    }

    // ── Happy paths ────────────────────────────────────────────────────────────

    @Test
    void GET_tasksList_ShouldSetNoCachePrivate() throws Exception {
        // my-tasks is per-user and uses ETag for revalidation
        handle("GET", "/api/v1/tasks/my-tasks");
        assertThat(response.getHeader("Cache-Control")).contains("no-cache");
        assertThat(response.getHeader("Cache-Control")).contains("private");
    }

    @Test
    void GET_availableTasks_ShouldSetNoCachePrivate() throws Exception {
        // available-tasks uses ETag for revalidation
        handle("GET", "/api/v1/tasks/available-tasks");
        assertThat(response.getHeader("Cache-Control")).contains("no-cache");
        assertThat(response.getHeader("Cache-Control")).contains("private");
    }

    @Test
    void GET_speciesMetadata_ShouldSetPrivate300s() throws Exception {
        handle("GET", "/api/v1/metadata/species");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("max-age=300, private");
    }

    @Test
    void GET_leaderboard_ShouldSetPublic60s() throws Exception {
        handle("GET", "/api/v1/gamification/leaderboard");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("max-age=60, public");
    }

    @Test
    void GET_gamificationUserInfo_ShouldSetPrivate30s() throws Exception {
        handle("GET", "/api/v1/gamification/user-info");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("max-age=30, private");
    }

    @Test
    void GET_userMe_ShouldSetNoCachePrivate() throws Exception {
        handle("GET", "/api/v1/users/me");
        // no-cache + private → "no-cache, private"
        assertThat(response.getHeader("Cache-Control")).contains("no-cache");
        assertThat(response.getHeader("Cache-Control")).contains("private");
    }

    @Test
    void GET_imageContent_ShouldSetPublic1Day() throws Exception {
        handle("GET", "/api/v1/images/42/content");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("max-age=86400, public");
    }

    @Test
    void GET_goldImage_ShouldSetPublic1Day() throws Exception {
        handle("GET", "/api/admin/gold-images/7/image");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("max-age=86400, public");
    }

    @Test
    void GET_collectionStats_ShouldSetPrivate60s() throws Exception {
        handle("GET", "/api/v1/collection/stats");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("max-age=60, private");
    }

    // ── Sensitive / no-store edge cases ───────────────────────────────────────

    @Test
    void POST_anyPath_ShouldSetNoStore() throws Exception {
        handle("POST", "/api/v1/tasks/create");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
    }

    @Test
    void PUT_anyPath_ShouldSetNoStore() throws Exception {
        handle("PUT", "/api/v1/tasks/1");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
    }

    @Test
    void DELETE_anyPath_ShouldSetNoStore() throws Exception {
        handle("DELETE", "/api/v1/tasks/1");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
    }

    @Test
    void GET_authEndpoint_ShouldSetNoStore() throws Exception {
        handle("GET", "/api/v1/auth/refresh");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
    }

    @Test
    void GET_health_ShouldSetNoStore() throws Exception {
        handle("GET", "/health");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
    }

    @Test
    void GET_nextBatch_ShouldSetNoStore() throws Exception {
        handle("GET", "/api/v1/classifications/next-batch");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
    }

    @Test
    void GET_unknownPath_ShouldNotSetCacheControlHeader() throws Exception {
        handle("GET", "/api/v1/some/unknown/endpoint");
        assertThat(response.getHeader("Cache-Control")).isNull();
    }
}
