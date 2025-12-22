package com.swipelab.service.classification;

import com.swipelab.service.user.CredibilityService;
import org.springframework.beans.factory.annotation.Autowired;

public class ClassificationService {
    @Autowired
    private CredibilityService credibilityService;

    public void submitClassification(String username, Long imageId, Long labelId) {
        // ... existing classification logic ...

        // Trigger credibility calculation
        credibilityService.onNewClassification(username, imageId);
    }
}
