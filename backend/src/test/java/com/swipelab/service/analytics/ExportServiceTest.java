// package com.swipelab.service.analytics;

// import com.swipelab.model.entity.*;
// import com.swipelab.repository.ClassificationRepository;
// import com.swipelab.repository.ImageRepository;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import java.io.ByteArrayOutputStream;
// import java.io.IOException;
// import java.time.LocalDateTime;
// import java.util.Arrays;
// import java.util.List;
// import java.util.Map;

// import static org.assertj.core.api.Assertions.assertThat;
// import static org.mockito.Mockito.when;

// @ExtendWith(MockitoExtension.class)
// public class ExportServiceTest {

// @Mock
// private ClassificationRepository classificationRepository;

// @Mock
// private ImageRepository imageRepository;

// @InjectMocks
// private ExportService exportService;

// private Task task;
// private Image image;
// private User user;
// private Label label;
// private Classification classification;

// @BeforeEach
// void setUp() {
// task = Task.builder().id(1L).title("Test Task").build();

// user = User.builder().username("testuser").email("test@example.com").build();
// label = Label.builder().id(1L).name("Yes").build();

// image = Image.builder()
// .id(1L)
// .imageUrl("http://example.com/image.jpg")
// .task(task)
// .isGoldStandard(false)
// .build();

// classification = Classification.builder()
// .id(1L)
// .image(image)
// .user(user)
// .label(label)
// .createdAt(LocalDateTime.of(2026, 1, 5, 10, 30, 0))
// .build();
// }

// @Test
// void testExportClassificationsAsCsv() throws IOException {
// when(imageRepository.findByTaskId(1L)).thenReturn(Arrays.asList(image));
// when(classificationRepository.findByImageId(1L)).thenReturn(Arrays.asList(classification));

// ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
// exportService.exportClassificationsAsCsv(1L, outputStream);

// String csv = outputStream.toString();

// assertThat(csv).contains("image_id,image_url,username,label,classified_at,is_gold_standard");
// assertThat(csv).contains("1,\"http://example.com/image.jpg\",\"testuser\",\"Yes\"");
// assertThat(csv).contains("false");
// }

// @Test
// void testExportClassificationsAsJson() {
// when(imageRepository.findByTaskId(1L)).thenReturn(Arrays.asList(image));
// when(classificationRepository.findByImageId(1L)).thenReturn(Arrays.asList(classification));

// List<Map<String, Object>> result =
// exportService.exportClassificationsAsJson(1L);

// assertThat(result).hasSize(1);
// assertThat(result.get(0).get("imageId")).isEqualTo(1L);
// assertThat(result.get(0).get("username")).isEqualTo("testuser");
// assertThat(result.get(0).get("label")).isEqualTo("Yes");
// assertThat(result.get(0).get("isGoldStandard")).isEqualTo(false);
// }

// @Test
// void testGetExportSummary() {
// when(imageRepository.findByTaskId(1L)).thenReturn(Arrays.asList(image));
// when(classificationRepository.findByImageId(1L)).thenReturn(Arrays.asList(classification));

// Map<String, Object> summary = exportService.getExportSummary(1L);

// assertThat(summary.get("taskId")).isEqualTo(1L);
// assertThat(summary.get("totalImages")).isEqualTo(1);
// assertThat(summary.get("totalClassifications")).isEqualTo(1);
// }
// }
