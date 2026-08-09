package com.swipelab.recipients.application;

import com.swipelab.recipients.dto.CreateRecipientGroupRequest;
import com.swipelab.recipients.dto.UpdateRecipientGroupRequest;
import com.swipelab.recipients.dto.RecipientGroupResponse;
import com.swipelab.exception.ResourceNotFoundException;
import com.swipelab.recipients.domain.RecipientGroup;
import com.swipelab.recipients.domain.RecipientUser;
import com.swipelab.recipients.infrastructure.RecipientGroupRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RecipientGroupService {

    private final RecipientGroupRepository recipientGroupRepository;
    private final com.swipelab.recipients.infrastructure.RecipientUserRepository recipientUserRepository;

    private Set<RecipientUser> getOrCreateRecipientUsers(List<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return new java.util.HashSet<>();
        }
        Set<RecipientUser> existingUsers = recipientUserRepository.findByUsernameIn(usernames);
        Set<String> existingUsernames = existingUsers.stream()
                .map(RecipientUser::getUsername)
                .collect(java.util.stream.Collectors.toSet());

        List<RecipientUser> newUsers = usernames.stream()
                .filter(u -> !existingUsernames.contains(u))
                .map(u -> RecipientUser.builder().username(u).active(true).build())
                .toList();

        if (!newUsers.isEmpty()) {
            existingUsers.addAll(recipientUserRepository.saveAll(newUsers));
        }
        return existingUsers;
    }

    @Transactional
    public RecipientGroupResponse createRecipientGroup(CreateRecipientGroupRequest request, String username) {
        if (recipientGroupRepository.existsByCreatedByAndName(username, request.getName())) {
            throw new IllegalStateException(
                    "Recipient group with name '" + request.getName() + "' already exists for your account");
        }

        Set<RecipientUser> users = getOrCreateRecipientUsers(request.getUsernames());

        RecipientGroup group = RecipientGroup.builder()
                .name(request.getName())
                .createdBy(username)
                .users(users)
                .build();

        recipientGroupRepository.save(group);
        return toRecipientGroupResponse(group);
    }

    @Transactional
    public RecipientGroupResponse updateRecipientGroup(Long groupId, UpdateRecipientGroupRequest request) {
        RecipientGroup group = recipientGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("RecipientGroup id:" + groupId + " not found"));

        if (request.getAddUsernames() != null && !request.getAddUsernames().isEmpty()) {
            Set<RecipientUser> usersToAdd = getOrCreateRecipientUsers(request.getAddUsernames());
            group.addUsers(usersToAdd);
        }

        if (request.getRemoveUsernames() != null && !request.getRemoveUsernames().isEmpty()) {
            Set<RecipientUser> usersToRemove = recipientUserRepository.findByUsernameIn(request.getRemoveUsernames());
            group.removeUsers(usersToRemove);
        }

        return toRecipientGroupResponse(group);
    }

    public List<RecipientGroupResponse> getRecipientGroups() {
        return recipientGroupRepository.findAll()
                .stream()
                .map(this::toRecipientGroupResponse)
                .toList();
    }

    @Transactional
    public void deleteRecipientGroup(Long groupId) {
        RecipientGroup group = recipientGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("RecipientGroup id:" + groupId + " not found"));
        recipientGroupRepository.delete(group);
    }

    private RecipientGroupResponse toRecipientGroupResponse(RecipientGroup group) {
        return RecipientGroupResponse.builder()
                .groupId(group.getId())
                .name(group.getName())
                .userCount(group.getUserCount())
                .usernames(
                        group.getUsers()
                                .stream()
                                .map(RecipientUser::getUsername)
                                .sorted()
                                .toList())
                .build();
    }
}









