package com.swipelab.auth.infrastructure;

import com.swipelab.auth.infrastructure.CustomOAuth2UserService;
import com.swipelab.auth.infrastructure.JwtAuthenticationFilter;
import com.swipelab.auth.infrastructure.BannedUserFilter;
import com.swipelab.auth.infrastructure.RateLimitingFilter;
import com.swipelab.auth.infrastructure.OAuth2AuthenticationFailureHandler;
import com.swipelab.auth.infrastructure.OAuth2AuthenticationSuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        @Autowired
        private JwtAuthenticationFilter jwtAuthenticationFilter;


        @Autowired
        private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

        @Autowired
        private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

        @Autowired
        private CustomOAuth2UserService customOAuth2UserService;

        @Autowired
        private BannedUserFilter bannedUserFilter;

        @Autowired
        private RateLimitingFilter rateLimitingFilter;

        @Autowired
        private org.springframework.core.env.Environment env;

        @Value("${cors.allowed-origins}")
        private String allowedOrigins;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(csrf -> csrf.disable())
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                                        response.setContentType("application/json");
                                                        response.getWriter().write(
                                                                        "{\"error\": \"Unauthorized\", \"message\": \""
                                                                                        + authException.getMessage()
                                                                                        + "\"}");
                                                }))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> {
                                        auth.requestMatchers(
                                                                "/",
                                                                "/error",
                                                                "/favicon.ico",
                                                                // Auth endpoints (strictly matched)
                                                                "/api/v1/auth/login",
                                                                "/api/v1/auth/register",
                                                                "/api/v1/auth/refresh",
                                                                "/api/v1/auth/password/forgot",
                                                                "/api/v1/auth/password/reset",
                                                                "/api/v1/auth/email/verify",
                                                                "/api/v1/auth/email/resend",
                                                                "/api/v1/auth/login/google",
                                                                "/api/v1/auth/external/stardbi/loginExternal",
                                                                "/api/v1/auth/verify-email", // HTML view endpoint
                                                                // System and Swagger
                                                                "/oauth2/**",
                                                                "/login/**",
                                                                "/stardbi/**", // Mock stardbi (usually for dev only)
                                                                "/api/admin/gold-images/*/image",
                                                                "/api/v1/images/*/content"
                                                ).permitAll();

                                        // MED-01: Expose Swagger UI only in non-prod environments
                                        if (!Arrays.asList(env.getActiveProfiles()).contains("prod")) {
                                                auth.requestMatchers(
                                                        "/v3/api-docs/**",
                                                        "/swagger-ui/**",
                                                        "/swagger-ui.html"
                                                ).permitAll();
                                        }

                                        auth.requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**")
                                                        .permitAll()
                                                        .anyRequest().authenticated();
                                })
                                .oauth2Login(oauth2 -> oauth2
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(customOAuth2UserService))
                                                .authorizationEndpoint(authorization -> authorization
                                                                .baseUri("/oauth2/authorize"))
                                                .redirectionEndpoint(redirection -> redirection
                                                                .baseUri("/oauth2/callback/**"))
                                                .successHandler(oAuth2AuthenticationSuccessHandler)
                                                .failureHandler(oAuth2AuthenticationFailureHandler))
                                // JWT filter must be added first so it can be used as an anchor
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                                // Rate limiting runs first — reject abusive IPs before any token work
                                .addFilterBefore(rateLimitingFilter, JwtAuthenticationFilter.class)
                                // Ban check runs last — after auth filter has set the SecurityContext
                                .addFilterAfter(bannedUserFilter, JwtAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                // Handle potential null/empty allowedOrigins
                if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
                        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
                } else {
                        configuration.setAllowedOrigins(List.of("*")); // Fallback
                }
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}