// package com.swipelab.service.analytics;

// import com.swipelab.dto.response.TaskAnalyticsResponse;
// import com.swipelab.dto.response.UserPerformanceResponse;
// import com.swipelab.model.entity.Classification;
// import com.swipelab.model.entity.Image;
// import com.swipelab.model.entity.Task;
// import com.swipelab.model.entity.User;
// import com.swipelab.repository.ClassificationRepository;
// import com.swipelab.repository.ImageRepository;
// import com.swipelab.repository.TaskRepository;
// import com.swipelab.repository.UserRepository;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import java.util.*;

// import static org.assertj.core.api.Assertions.assertThat;
// import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class)
// public class AnalyticsServiceTest {

// @Mock
// private TaskRepository taskRepository;

// @Mock
// private ImageRepository imageRepository;

// @Mock
// private ClassificationRepository classificationRepository;

// @Mock
// private UserRepository userRepository;

// @InjectMocks
// private AnalyticsService analyticsService;

// private Task task;
// private List<Image> images;
// private User user;

// @BeforeEach
// void setUp() {
// task = Task.builder().id(1L).title("Test Task").build();

// images = new ArrayList<>();
// for (int i = 1; i <= 10; i++) {
// images.add(Image.builder().id((long) i).task(task).build());
// }

// user = User.builder()
// .username("testuser")
// .displayName("Test User")
// .totalClassifications(50)
// .totalGoldClassifications(10)
// .correctGoldClassifications(8)
// .credibilityScore(0.8)
// .currentStreak(5)
// .points(500L)
// .build();
// }

// @Test
// void testGetTaskAnalytics_BasicMetrics() {
// when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
// when(imageRepository.findByTaskId(1L)).thenReturn(images);
// when(classificationRepository.findByImageId(anyLong())).thenReturn(Collections.emptyList());

// TaskAnalyticsResponse response = analyticsService.getTaskAnalytics(1L);

// assertThat(response).isNotNull();
// assertThat(response.getTaskId()).isEqualTo(1L);
// assertThat(response.getTotalImages()).isEqualTo(10);
// assertThat(response.getClassifiedImages()).isEqualTo(0);
// }

// @Test
// void testGetUserPerformanceMetrics() {
// when(userRepository.findAll()).thenReturn(Arrays.asList(user));

// List<UserPerformanceResponse> responses =
// analyticsService.getUserPerformanceMetrics(null);

// assertThat(responses).hasSize(1);
// assertThat(responses.get(0).getUsername()).isEqualTo("testuser");
// assertThat(responses.get(0).getGoldAccuracy()).isEqualTo(80.0);
// }

// @Test
// void testGetTopPerformers() {
// when(userRepository.findAll()).thenReturn(Arrays.asList(user));

// List<UserPerformanceResponse> responses =
// analyticsService.getTopPerformers(5);

// assertThat(responses).isNotEmpty();
// assertThat(responses.get(0).getUsername()).isEqualTo("testuser");
// }
// }
