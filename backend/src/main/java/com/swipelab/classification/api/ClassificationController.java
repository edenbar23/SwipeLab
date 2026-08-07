package com.swipelab.classification.api;

import com.swipelab.classification.dto.UserClassification;
import com.swipelab.classification.dto.api.NextBatchResponse;
import com.swipelab.classification.dto.api.SubmitClassificationRequest;
import com.swipelab.dto.request.ClassificationRequest;
import com.swipelab.classification.application.ClassificationService;
import com.swipelab.classification.domain.image.ImageService;
import com.swipelab.users.application.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/classifications")
@RequiredArgsConstructor
public class ClassificationController {

        private final ClassificationService classificationService;
        private final ImageService imageService;
        private final UserService userService;

        // 2.1 Get Next Batch
        @GetMapping("/next-batch")
        public ResponseEntity<NextBatchResponse> getNextBatch(
                        @RequestParam(defaultValue = "10") int count,
                        @RequestParam(required = false) Long taskId,
                        @AuthenticationPrincipal UserDetails userDetails) {

                Long effectiveTaskId = (taskId != null) ? taskId : 1L;

                return ResponseEntity
                                .ok(imageService.getNextBatchForApi(effectiveTaskId, userDetails.getUsername(), count));
        }

        // 2.2 Submit Classification — frontend calls POST
        // /api/v1/classifications/submit
        @PostMapping("/submit")
        public ResponseEntity<NextBatchResponse> submitClassificationDirect(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @Valid @RequestBody SubmitClassificationRequest request) {
                log.info("Entered submit endpoint for imageId: {}, taskId: {}, decision: {}",
                                request.getImageId(), request.getTaskId(), request.getDecision());
                try {
                        String role = userDetails.getAuthorities().stream()
                                        .findFirst()
                                        .map(a -> a.getAuthority())
                                        .orElse("USER");
                        Double credibility = userService.getUserCredibility(userDetails.getUsername());
                        NextBatchResponse response = classificationService.submitClassification(
                                        userDetails.getUsername(), role, credibility, request);
                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        log.error("Fatal error in submit endpoint: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }
        }

        // 2.2b Submit Classification — legacy path with classificationId path variable
        @PostMapping("/{classificationId}/submit")
        public ResponseEntity<NextBatchResponse> submitClassification(
                        @PathVariable Long classificationId,
                        @AuthenticationPrincipal UserDetails userDetails,
                        @Valid @RequestBody SubmitClassificationRequest request) {
                log.info("Entered submit (legacy) endpoint for classificationId: {}", classificationId);
                try {
                        String role = userDetails.getAuthorities().stream()
                                        .findFirst()
                                        .map(a -> a.getAuthority())
                                        .orElse("USER");
                        Double credibility = userService.getUserCredibility(userDetails.getUsername());
                        NextBatchResponse response = classificationService.submitClassification(
                                        userDetails.getUsername(), role, credibility, request);
                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        log.error("Fatal error in submit (legacy) endpoint: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }
        }

        // 2.3 Play Task (Initialize Session)
        @PostMapping("/tasks/{taskId}/play")
        public ResponseEntity<NextBatchResponse> playTask(
                        @PathVariable Long taskId,
                        @RequestParam(defaultValue = "10") int count,
                        @AuthenticationPrincipal UserDetails userDetails) {
                log.info("Entered play endpoint for task: {}, user: {}", taskId, userDetails.getUsername());
                try {
                        NextBatchResponse response = imageService.getNextBatchForApi(taskId, userDetails.getUsername(),
                                        count);
                        log.info("Play endpoint returning {} images for task: {}", response.getImages().size(), taskId);
                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        log.error("Fatal error in play endpoint for task {}: ", taskId, e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }
        }

        @PostMapping("/tasks/{taskId}/batch")
        public ResponseEntity<Void> submitBatchClassificationsLegacy(
                        @PathVariable Long taskId,
                        @AuthenticationPrincipal UserDetails userDetails,
                        @RequestBody List<@Valid ClassificationRequest> requests) {

                List<UserClassification> userClassifications = requests.stream()
                                .map(req -> new UserClassification(req.getImageId(), req.getUserResponse()))
                                .collect(Collectors.toList());

                String role = userDetails.getAuthorities().stream()
                                .findFirst()
                                .map(a -> a.getAuthority())
                                .orElse("USER");

                classificationService.submitBatchResponses(userDetails.getUsername(), role, taskId,
                                userClassifications);

                return ResponseEntity.status(HttpStatus.CREATED).build();
        }
}
