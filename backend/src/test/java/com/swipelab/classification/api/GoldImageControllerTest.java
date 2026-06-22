package com.swipelab.classification.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swipelab.classification.domain.GoldImageService;
import com.swipelab.dto.request.GoldImageRequest;
import com.swipelab.dto.response.GoldImageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GoldImageControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private GoldImageService goldImageService;

    @InjectMocks
    private GoldImageController goldImageController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(goldImageController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getAllGoldImages_ShouldReturnListOfGoldImageResponse() throws Exception {
        GoldImageResponse response = GoldImageResponse.builder().id(1L).build();
        when(goldImageService.getAllGoldImages()).thenReturn(Collections.singletonList(response));

        mockMvc.perform(get("/api/admin/gold-images/get-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void createGoldImage_ShouldReturnCreatedGoldImage_WhenValidRequest() throws Exception {
        GoldImageRequest request = new GoldImageRequest();
        request.setImageId(1L);
        request.setSpecies("Lion");
        request.setCorrectAnswer(com.swipelab.classification.domain.GoldImage.UserResponse.YES);
        
        GoldImageResponse response = GoldImageResponse.builder().id(1L).build();
        when(goldImageService.createGoldImage(any(GoldImageRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/gold-images")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getGoldImagesByTask_ShouldReturnListOfGoldImageResponse() throws Exception {
        GoldImageResponse response = GoldImageResponse.builder().id(1L).build();
        when(goldImageService.getGoldImagesByTask(1L)).thenReturn(Collections.singletonList(response));

        mockMvc.perform(get("/api/admin/gold-images")
                .param("taskId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void getGoldImageById_ShouldReturnGoldImageResponse() throws Exception {
        GoldImageResponse response = GoldImageResponse.builder().id(1L).build();
        when(goldImageService.getGoldImageById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/admin/gold-images/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void updateGoldImage_ShouldReturnUpdatedGoldImage() throws Exception {
        GoldImageRequest request = new GoldImageRequest();
        GoldImageResponse response = GoldImageResponse.builder().id(1L).build();
        
        when(goldImageService.updateGoldImage(eq(1L), any(GoldImageRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/admin/gold-images/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void deleteGoldImage_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/gold-images/1"))
                .andExpect(status().isNoContent());

        verify(goldImageService, times(1)).deleteGoldImage(1L);
    }

}


