package com.swipelab.classification.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swipelab.classification.application.ClassificationService;
import com.swipelab.classification.domain.ImageService;
import com.swipelab.classification.dto.UserClassification;
import com.swipelab.classification.dto.api.NextBatchResponse;
import com.swipelab.classification.dto.api.SubmitClassificationRequest;
import com.swipelab.dto.request.ClassificationRequest;
import com.swipelab.users.application.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ClassificationControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ClassificationService classificationService;

    @Mock
    private ImageService imageService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ClassificationController classificationController;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(classificationController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();

        userDetails = new User("testuser", "password", Collections.singletonList(new SimpleGrantedAuthority("USER")));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void getNextBatch_ShouldReturnNextBatchResponse() throws Exception {
        NextBatchResponse batchResponse = NextBatchResponse.builder().build();
        when(imageService.getNextBatchForApi(anyLong(), anyString(), anyInt())).thenReturn(batchResponse);

        mockMvc.perform(get("/api/v1/classifications/next-batch")
                .param("count", "10")
                .param("taskId", "1"))
                .andExpect(status().isOk());

        verify(imageService, times(1)).getNextBatchForApi(1L, "testuser", 10);
    }

    @Test
    void submitClassification_ShouldReturnNextBatchResponse_WhenValidRequest() throws Exception {
        SubmitClassificationRequest request = new SubmitClassificationRequest();
        request.setImageId(1L);
        request.setTaskId(1L);
        request.setDecision(com.swipelab.classification.domain.Classification.UserResponse.YES);
        
        NextBatchResponse batchResponse = NextBatchResponse.builder().build();

        when(userService.getUserCredibility(anyString())).thenReturn(0.8);
        when(classificationService.submitClassification(anyString(), anyString(), anyDouble(), any(SubmitClassificationRequest.class)))
                .thenReturn(batchResponse);

        mockMvc.perform(post("/api/v1/classifications/1/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(classificationService, times(1)).submitClassification(eq("testuser"), eq("USER"), eq(0.8), any(SubmitClassificationRequest.class));
    }

    @Test
    void submitBatchClassificationsLegacy_ShouldReturnCreated_WhenValidRequest() throws Exception {
        ClassificationRequest req = new ClassificationRequest();
        req.setImageId(1L);
        req.setUserResponse(com.swipelab.classification.domain.Classification.UserResponse.YES);
        
        List<ClassificationRequest> requests = Collections.singletonList(req);

        mockMvc.perform(post("/api/v1/classifications/tasks/1/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isCreated());

        verify(classificationService, times(1)).submitBatchResponses(eq("testuser"), eq("USER"), eq(1L), anyList());
    }
}
