package com.swipelab.recipients.application;

import com.swipelab.dto.request.CreateRecipientGroupRequest;
import com.swipelab.dto.request.UpdateRecipientGroupRequest;
import com.swipelab.dto.response.RecipientGroupResponse;
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

    @Transactional
    public RecipientGroupResponse createRecipientGroup(CreateRecipientGroupRequest request) {
        if (recipientGroupRepository.existsByName(request.getName())) {
            throw new IllegalStateException(
                    "Recipient group with name '" + request.getName() + "' already exists");
        }

        Set<RecipientUser> users = recipientUserRepository.findByUsernameIn(request.getUsernames());

        RecipientGroup group = RecipientGroup.builder()
                .name(request.getName())
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
            Set<RecipientUser> usersToAdd = recipientUserRepository.findByUsernameIn(request.getAddUsernames());
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
