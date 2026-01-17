package com.swipelab.classification.api;

import com.swipelab.dto.request.ClassificationRequest;
import com.swipelab.dto.response.ClassificationResponse;
import com.swipelab.classification.application.ClassificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/classifications")
@RequiredArgsConstructor
public class ClassificationController {

    private final ClassificationService classificationService;

    @PostMapping
    public ResponseEntity<ClassificationResponse> submitClassification(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ClassificationRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(classificationService.submitClassification(userDetails.getUsername(), request));
    }
}
