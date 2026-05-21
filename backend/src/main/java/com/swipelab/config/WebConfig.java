package com.swipelab.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Static file serving for /uploads/** has been removed intentionally.
// Uploaded images are served through the authenticated API endpoint
// GET /api/admin/gold-images/{id}/image to avoid exposing filesystem structure.
@Configuration
public class WebConfig implements WebMvcConfigurer {
}
