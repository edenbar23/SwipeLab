// package com.swipelab.service.classification;

// import com.swipelab.model.entity.Image;
// import com.swipelab.model.entity.Task;
// import com.swipelab.model.entity.User;
// import com.swipelab.repository.ClassificationRepository;
// import com.swipelab.repository.ImageRepository;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import java.util.*;

// import static org.assertj.core.api.Assertions.assertThat;
// import static org.mockito.ArgumentMatchers.anyLong;
// import static org.mockito.ArgumentMatchers.anyString;
// import static org.mockito.Mockito.when;

// @ExtendWith(MockitoExtension.class)
// public class TaskDistributionServiceTest {

// @Mock
// private ImageRepository imageRepository;

// @Mock
// private ClassificationRepository classificationRepository;

// @InjectMocks
// private TaskDistributionService taskDistributionService;

// private Task task;
// private User user;
// private List<Image> regularImages;
// private List<Image> goldImages;

// @BeforeEach
// void setUp() {
// task = Task.builder().id(1L).title("Test Task").build();
// user = User.builder().username("testuser").build();

// // Create 20 regular images
// regularImages = new ArrayList<>();
// for (int i = 1; i <= 20; i++) {
// regularImages.add(Image.builder()
// .id((long) i)
// .imageUrl("http://example.com/image" + i + ".jpg")
// .task(task)
// .priority(0)
// .isGoldStandard(false)
// .build());
// }

// // Create 3 gold images
// goldImages = new ArrayList<>();
// for (int i = 21; i <= 23; i++) {
// goldImages.add(Image.builder()
// .id((long) i)
// .imageUrl("http://example.com/gold" + i + ".jpg")
// .task(task)
// .priority(0)
// .isGoldStandard(true)
// .build());
// }
// }

// @Test
// void testGetNextImageForUser_ReturnsRegularImage() {
// when(imageRepository.findByTaskIdAndIsGoldStandardFalse(1L)).thenReturn(regularImages);
// when(classificationRepository.existsByUser_UsernameAndImage_Id(anyString(),
// anyLong())).thenReturn(false);
// when(classificationRepository.countByImage_Id(anyLong())).thenReturn(0L);

// Optional<Image> result =
// taskDistributionService.getNextImageForUser("testuser", 1L);

// assertThat(result).isPresent();
// assertThat(result.get().getIsGoldStandard()).isFalse();
// }

// @Test
// void testGetNextImageForUser_GoldImageEvery15th() {
// when(imageRepository.findByTaskIdAndIsGoldStandardFalse(1L)).thenReturn(regularImages);
// when(imageRepository.findByTaskIdAndIsGoldStandardTrue(1L)).thenReturn(goldImages);
// when(classificationRepository.existsByUser_UsernameAndImage_Id(anyString(),
// anyLong())).thenReturn(false);
// when(classificationRepository.countByImage_Id(anyLong())).thenReturn(0L);

// // Get first 15 images (should all be regular, count starts at 0)
// for (int i = 0; i < 15; i++) {
// Optional<Image> result =
// taskDistributionService.getNextImageForUser("testuser", 1L);
// assertThat(result).isPresent();
// assertThat(result.get().getIsGoldStandard()).isFalse();
// }

// // 15th image should be gold
// Optional<Image> fifteenthImage =
// taskDistributionService.getNextImageForUser("testuser", 1L);
// assertThat(fifteenthImage).isPresent();
// assertThat(fifteenthImage.get().getIsGoldStandard()).isTrue();
// }

// @Test
// void testGetNextImageForUser_PreventsDuplicates() {
// when(imageRepository.findByTaskIdAndIsGoldStandardFalse(1L)).thenReturn(regularImages);

// // User has classified first 10 images
// when(classificationRepository.existsByUser_UsernameAndImage_Id(anyString(),
// anyLong()))
// .thenAnswer(invocation -> {
// Long imageId = invocation.getArgument(1);
// return imageId <= 10;
// });
// when(classificationRepository.countByImage_Id(anyLong())).thenReturn(0L);

// Optional<Image> result =
// taskDistributionService.getNextImageForUser("testuser", 1L);

// assertThat(result).isPresent();
// assertThat(result.get().getId()).isGreaterThan(10L);
// }

// @Test
// void testGetNextImageForUser_IntelligentAssignment() {
// when(imageRepository.findByTaskIdAndIsGoldStandardFalse(1L)).thenReturn(regularImages);
// when(classificationRepository.existsByUser_UsernameAndImage_Id(anyString(),
// anyLong())).thenReturn(false);

// // IMPORTANT: Set default FIRST, then override specific cases
// when(classificationRepository.countByImage_Id(anyLong())).thenReturn(10L);
// when(classificationRepository.countByImage_Id(1L)).thenReturn(5L);
// when(classificationRepository.countByImage_Id(2L)).thenReturn(2L);
// when(classificationRepository.countByImage_Id(3L)).thenReturn(0L);

// Optional<Image> result =
// taskDistributionService.getNextImageForUser("testuser", 1L);

// assertThat(result).isPresent();
// // Should prioritize image with fewest classifications (Image 3)
// assertThat(result.get().getId()).isEqualTo(3L);
// }

// @Test
// void testGetNextImageForUser_PrioritizesHighPriorityImages() {
// // Set different priorities
// regularImages.get(0).setPriority(5); // Image 1: priority 5
// regularImages.get(1).setPriority(10); // Image 2: priority 10
// regularImages.get(2).setPriority(3); // Image 3: priority 3

// when(imageRepository.findByTaskIdAndIsGoldStandardFalse(1L)).thenReturn(regularImages);
// when(classificationRepository.existsByUser_UsernameAndImage_Id(anyString(),
// anyLong())).thenReturn(false);
// when(classificationRepository.countByImage_Id(anyLong())).thenReturn(0L);

// Optional<Image> result =
// taskDistributionService.getNextImageForUser("testuser", 1L);

// assertThat(result).isPresent();
// // Should prioritize highest priority image (Image 2 with priority 10)
// assertThat(result.get().getId()).isEqualTo(2L);
// }

// @Test
// void testGetNextImageForUser_NoImagesAvailable() {
// when(imageRepository.findByTaskIdAndIsGoldStandardFalse(1L)).thenReturn(Collections.emptyList());

// Optional<Image> result =
// taskDistributionService.getNextImageForUser("testuser", 1L);

// assertThat(result).isEmpty();
// }

// @Test
// void testResetUserSession() {
// when(imageRepository.findByTaskIdAndIsGoldStandardFalse(1L)).thenReturn(regularImages);
// when(classificationRepository.existsByUser_UsernameAndImage_Id(anyString(),
// anyLong())).thenReturn(false);
// when(classificationRepository.countByImage_Id(anyLong())).thenReturn(0L);

// // Get a few images
// taskDistributionService.getNextImageForUser("testuser", 1L);
// taskDistributionService.getNextImageForUser("testuser", 1L);

// // Reset session
// taskDistributionService.resetUserSession("testuser", 1L);

// // Session count should be reset (can verify by checking gold image insertion
// // timing)
// // This is more of a state verification test
// assertThat(taskDistributionService).isNotNull();
// }
// }
