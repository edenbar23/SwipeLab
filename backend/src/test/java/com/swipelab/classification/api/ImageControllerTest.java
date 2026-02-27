package com.swipelab.classification.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swipelab.classification.domain.ImageService;
import com.swipelab.dto.request.ImageUploadRequest;
import com.swipelab.dto.response.ImageBatchResponse;
import com.swipelab.dto.response.ImageResponse;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ImageControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ImageService imageService;

    @InjectMocks
    private ImageController imageController;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(imageController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();

        userDetails = new User("testuser", "password", Collections.singletonList(new SimpleGrantedAuthority("USER")));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void uploadImage_ShouldReturnCreatedImage() throws Exception {
        ImageUploadRequest request = new ImageUploadRequest();
        request.setTaskId(1L);
        // Populating required fields to bypass rudimentary validation mocks if any
        request.setImageUrl("http://example.com/img.png");

        ImageResponse response = ImageResponse.builder().id(1L).build();
        when(imageService.uploadImage(any(ImageUploadRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/images/upload")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getImageBatch_ShouldReturnImageBatchResponse() throws Exception {
        ImageBatchResponse response = ImageBatchResponse.builder().build();
        when(imageService.getImageBatch(eq(1L), eq("testuser"))).thenReturn(response);

        mockMvc.perform(get("/api/v1/images/batch")
                .param("taskId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void getImageById_ShouldReturnImageResponse() throws Exception {
        ImageResponse response = ImageResponse.builder().id(1L).build();
        when(imageService.getImageById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/images/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }
}
