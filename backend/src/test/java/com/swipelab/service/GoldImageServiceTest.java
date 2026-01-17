// package com.swipelab.service;

// import com.swipelab.dto.request.GoldImageRequest;
// import com.swipelab.dto.response.GoldImageResponse;
// import com.swipelab.model.entity.GoldImage;
// import com.swipelab.model.entity.Image;
// import com.swipelab.model.entity.Label;
// import com.swipelab.model.entity.Task;
// import com.swipelab.repository.GoldImageRepository;
// import com.swipelab.repository.ImageRepository;
// import com.swipelab.repository.LabelRepository;
// import com.swipelab.repository.TaskRepository;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import java.time.LocalDateTime;
// import java.util.Arrays;
// import java.util.List;
// import java.util.Optional;

// import static org.assertj.core.api.Assertions.assertThat;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class)
// public class GoldImageServiceTest {

// @Mock
// private GoldImageRepository goldImageRepository;

// @Mock
// private ImageRepository imageRepository;

// @Mock
// private TaskRepository taskRepository;

// @Mock
// private LabelRepository labelRepository;

// @InjectMocks
// private GoldImageService goldImageService;

// private Task task;
// private Label label;
// private Image image;
// private GoldImage goldImage;

// @BeforeEach
// void setUp() {
// task = Task.builder().id(1L).title("Test Task").build();
// label = Label.builder().id(1L).name("Test Label").build();
// image = Image.builder()
// .id(1L)
// .imageUrl("http://example.com/image.jpg")
// .task(task)
// .isGoldStandard(true)
// .correctLabel(label)
// .build();
// goldImage = GoldImage.builder()
// .id(1L)
// .image(image)
// .difficultyLevel("MEDIUM")
// .explanation("Test explanation")
// .createdAt(LocalDateTime.now())
// .build();
// }

// @Test
// void testCreateGoldImage() {
// GoldImageRequest request = GoldImageRequest.builder()
// .imageUrl("http://example.com/new.jpg")
// .caption("Test caption")
// .taskId(1L)
// .correctLabelId(1L)
// .difficultyLevel("HARD")
// .explanation("Test explanation")
// .build();

// when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
// when(labelRepository.findById(1L)).thenReturn(Optional.of(label));
// when(imageRepository.save(any(Image.class))).thenReturn(image);
// when(goldImageRepository.save(any(GoldImage.class))).thenReturn(goldImage);

// GoldImageResponse response = goldImageService.createGoldImage(request);

// assertThat(response).isNotNull();
// assertThat(response.getDifficultyLevel()).isEqualTo("MEDIUM");
// verify(imageRepository).save(any(Image.class));
// verify(goldImageRepository).save(any(GoldImage.class));
// }

// @Test
// void testGetGoldImagesByTask() {
// when(goldImageRepository.findByImage_Task_Id(1L)).thenReturn(Arrays.asList(goldImage));

// List<GoldImageResponse> responses = goldImageService.getGoldImagesByTask(1L);

// assertThat(responses).hasSize(1);
// assertThat(responses.get(0).getDifficultyLevel()).isEqualTo("MEDIUM");
// }

// @Test
// void testUpdateGoldImage() {
// GoldImageRequest request = GoldImageRequest.builder()
// .difficultyLevel("EASY")
// .explanation("Updated explanation")
// .build();

// when(goldImageRepository.findById(1L)).thenReturn(Optional.of(goldImage));
// when(goldImageRepository.save(any(GoldImage.class))).thenReturn(goldImage);

// GoldImageResponse response = goldImageService.updateGoldImage(1L, request);

// assertThat(response).isNotNull();
// verify(goldImageRepository).save(any(GoldImage.class));
// }

// @Test
// void testDeleteGoldImage() {
// when(goldImageRepository.findById(1L)).thenReturn(Optional.of(goldImage));

// goldImageService.deleteGoldImage(1L);

// verify(imageRepository).save(any(Image.class));
// verify(goldImageRepository).delete(goldImage);
// }
// }
