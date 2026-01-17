package com.swipelab.controller;

import com.swipelab.classification.domain.Label;

import com.swipelab.classification.application.LabelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/labels")
@RequiredArgsConstructor
public class LabelController {

    private final LabelService labelService;

    @GetMapping
    public ResponseEntity<List<Label>> getAllLabels() {
        return ResponseEntity.ok(labelService.getAllLabels());
    }
}
