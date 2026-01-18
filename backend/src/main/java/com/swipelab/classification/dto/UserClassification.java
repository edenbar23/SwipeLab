package com.swipelab.classification.dto;

import com.swipelab.classification.domain.Classification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserClassification {
    private Long imageId;
    private Classification.UserResponse userResponse;
}
