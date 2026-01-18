package com.swipelab.classification.domain;

import com.swipelab.classification.infrastructure.LabelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LabelService {

    private final LabelRepository labelRepository;

    /**
     * Retrieve all available labels.
     *
     * @return List of all Label entities.
     */
    public List<Label> getAllLabels() {
        return labelRepository.findAll();
    }
}
