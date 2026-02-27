package com.swipelab.recipients.application;

import com.swipelab.dto.request.CreateRecipientGroupRequest;
import com.swipelab.dto.request.UpdateRecipientGroupRequest;
import com.swipelab.dto.response.RecipientGroupResponse;
import com.swipelab.exception.ResourceNotFoundException;
import com.swipelab.recipients.domain.RecipientGroup;
import com.swipelab.recipients.domain.RecipientUser;
import com.swipelab.recipients.infrastructure.RecipientGroupRepository;
import com.swipelab.recipients.infrastructure.RecipientUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipientGroupServiceTest {

    @Mock
    private RecipientGroupRepository recipientGroupRepository;

    @Mock
    private RecipientUserRepository recipientUserRepository;

    @InjectMocks
    private RecipientGroupService recipientGroupService;

    private RecipientGroup group;
    private RecipientUser user1;
    private RecipientUser user2;
    private Set<RecipientUser> initialUsers;

    @BeforeEach
    void setUp() {
        user1 = new RecipientUser();
        user1.setUsername("user1");
        
        user2 = new RecipientUser();
        user2.setUsername("user2");

        initialUsers = new HashSet<>(Arrays.asList(user1, user2));

        group = new RecipientGroup();
        group.setId(1L);
        group.setName("Group A");
        group.setUsers(initialUsers);
    }

    @Test
    void createRecipientGroup_ShouldReturnResponse_WhenValid() {
        CreateRecipientGroupRequest request = new CreateRecipientGroupRequest();
        request.setName("Group A");
        request.setUsernames(Arrays.asList("user1", "user2"));

        when(recipientGroupRepository.existsByName("Group A")).thenReturn(false);
        when(recipientUserRepository.findByUsernameIn(request.getUsernames())).thenReturn(initialUsers);
        when(recipientGroupRepository.save(any(RecipientGroup.class))).thenReturn(group);

        RecipientGroupResponse response = recipientGroupService.createRecipientGroup(request);

        assertNotNull(response);
        assertEquals("Group A", response.getName());
        assertEquals(2, response.getUserCount());
        assertTrue(response.getUsernames().contains("user1"));
    }

    @Test
    void createRecipientGroup_ShouldThrowException_WhenNameExists() {
        CreateRecipientGroupRequest request = new CreateRecipientGroupRequest();
        request.setName("Group A");

        when(recipientGroupRepository.existsByName("Group A")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> recipientGroupService.createRecipientGroup(request));
    }

    @Test
    void updateRecipientGroup_ShouldAddAndRemoveUsers() {
        UpdateRecipientGroupRequest request = new UpdateRecipientGroupRequest();
        request.setAddUsernames(Collections.singletonList("user3"));
        request.setRemoveUsernames(Collections.singletonList("user1"));

        RecipientUser user3 = new RecipientUser();
        user3.setUsername("user3");

        when(recipientGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(recipientUserRepository.findByUsernameIn(Collections.singletonList("user3"))).thenReturn(new HashSet<>(Collections.singletonList(user3)));
        when(recipientUserRepository.findByUsernameIn(Collections.singletonList("user1"))).thenReturn(new HashSet<>(Collections.singletonList(user1)));

        RecipientGroupResponse response = recipientGroupService.updateRecipientGroup(1L, request);

        assertNotNull(response);
        assertEquals(2, response.getUserCount()); // user1 removed, user2 left, user3 added
        assertTrue(response.getUsernames().contains("user2"));
        assertTrue(response.getUsernames().contains("user3"));
        assertFalse(response.getUsernames().contains("user1"));
    }

    @Test
    void updateRecipientGroup_ShouldThrowException_WhenGroupNotFound() {
        when(recipientGroupRepository.findById(1L)).thenReturn(Optional.empty());

        UpdateRecipientGroupRequest request = new UpdateRecipientGroupRequest();
        assertThrows(ResourceNotFoundException.class, () -> recipientGroupService.updateRecipientGroup(1L, request));
    }

    @Test
    void getRecipientGroups_ShouldReturnList() {
        when(recipientGroupRepository.findAll()).thenReturn(Collections.singletonList(group));

        List<RecipientGroupResponse> responses = recipientGroupService.getRecipientGroups();

        assertEquals(1, responses.size());
        assertEquals("Group A", responses.get(0).getName());
    }

    @Test
    void deleteRecipientGroup_ShouldDelete_WhenExists() {
        when(recipientGroupRepository.findById(1L)).thenReturn(Optional.of(group));

        recipientGroupService.deleteRecipientGroup(1L);

        verify(recipientGroupRepository, times(1)).delete(group);
    }

    @Test
    void deleteRecipientGroup_ShouldThrowException_WhenNotFound() {
        when(recipientGroupRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> recipientGroupService.deleteRecipientGroup(1L));
    }
}
