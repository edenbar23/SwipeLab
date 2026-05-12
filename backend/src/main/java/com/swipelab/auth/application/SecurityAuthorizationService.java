package com.swipelab.auth.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service("securityAuthorizationService")
public class SecurityAuthorizationService {

    @Value("${app.security.super-admin.username}")
    private String superAdminUsername;

    public boolean isSuperAdmin(String username) {
        if (username == null || superAdminUsername == null) {
            return false;
        }
        return superAdminUsername.equalsIgnoreCase(username);
    }
}
