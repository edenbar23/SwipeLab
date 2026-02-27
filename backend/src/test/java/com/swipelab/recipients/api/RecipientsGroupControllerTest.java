package com.swipelab.recipients.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swipelab.dto.request.CreateRecipientGroupRequest;
import com.swipelab.dto.request.UpdateRecipientGroupRequest;
import com.swipelab.dto.response.RecipientGroupResponse;
import com.swipelab.recipients.application.RecipientGroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RecipientsGroupControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private RecipientGroupService recipientGroupService;

    @InjectMocks
    private RecipientsGroupController recipientsGroupController;

    private RecipientGroupResponse response;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(recipientsGroupController).build();
        objectMapper = new ObjectMapper();

        response = RecipientGroupResponse.builder()
                .groupId(1L)
                .name("Group A")
                .userCount(2)
                .usernames(Arrays.asList("user1", "user2"))
                .build();
    }

    @Test
    void createRecipientGroup_ShouldReturnCreated() throws Exception {
        CreateRecipientGroupRequest request = new CreateRecipientGroupRequest();
        request.setName("Group A");
        request.setUsernames(Arrays.asList("user1", "user2"));

        when(recipientGroupService.createRecipientGroup(any(CreateRecipientGroupRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/dashboard/recipients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groupId").value(1L))
                .andExpect(jsonPath("$.name").value("Group A"));
    }

    @Test
    void updateRecipientGroup_ShouldReturnOk() throws Exception {
        UpdateRecipientGroupRequest request = new UpdateRecipientGroupRequest();
        request.setAddUsernames(Collections.singletonList("user3"));

        when(recipientGroupService.updateRecipientGroup(eq(1L), any(UpdateRecipientGroupRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/dashboard/recipients/{groupId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(1L));
    }

    @Test
    void getRecipientGroups_ShouldReturnList() throws Exception {
        when(recipientGroupService.getRecipientGroups()).thenReturn(Collections.singletonList(response));

        mockMvc.perform(get("/api/v1/dashboard/recipients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].groupId").value(1L))
                .andExpect(jsonPath("$[0].name").value("Group A"));
    }

    @Test
    void deleteRecipientGroup_ShouldReturnNoContent() throws Exception {
        doNothing().when(recipientGroupService).deleteRecipientGroup(1L);

        mockMvc.perform(delete("/api/v1/dashboard/recipients/{groupId}", 1L))
                .andExpect(status().isNoContent());
    }
}
