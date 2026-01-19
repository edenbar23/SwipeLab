package com.swipelab.recipients.api;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.swipelab.dto.request.CreateRecipientGroupRequest;
import com.swipelab.dto.request.UpdateRecipientGroupRequest;
import com.swipelab.dto.response.RecipientGroupResponse;
import com.swipelab.recipients.application.RecipientGroupService;

@RestController
@RequestMapping("/api/v1/dashboard/recipients")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class RecipientsGroupController {
    private final RecipientGroupService recipientGroupService;

    @PostMapping
    public ResponseEntity<RecipientGroupResponse> createRecipientGroup(
            @RequestBody CreateRecipientGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recipientGroupService.createRecipientGroup(request));
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<RecipientGroupResponse> updateRecipientGroup(
            @PathVariable Long groupId,
            @RequestBody UpdateRecipientGroupRequest request) {
        return ResponseEntity.ok(recipientGroupService.updateRecipientGroup(groupId, request));
    }

    @GetMapping
    public ResponseEntity<List<RecipientGroupResponse>> getRecipientGroups() {
        return ResponseEntity.ok(recipientGroupService.getRecipientGroups());
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteRecipientGroup(@PathVariable Long groupId) {
        recipientGroupService.deleteRecipientGroup(groupId);
        return ResponseEntity.noContent().build();
    }
}
