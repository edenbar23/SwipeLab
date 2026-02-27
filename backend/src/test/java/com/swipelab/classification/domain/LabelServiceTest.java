package com.swipelab.classification.domain;

import com.swipelab.classification.infrastructure.LabelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabelServiceTest {

    @Mock
    private LabelRepository labelRepository;

    @InjectMocks
    private LabelService labelService;

    @Test
    void getAllLabels_ShouldReturnList() {
        Label label1 = new Label();
        label1.setId(1L);
        Label label2 = new Label();
        label2.setId(2L);

        when(labelRepository.findAll()).thenReturn(Arrays.asList(label1, label2));

        List<Label> labels = labelService.getAllLabels();

        assertEquals(2, labels.size());
        verify(labelRepository, times(1)).findAll();
    }
}
