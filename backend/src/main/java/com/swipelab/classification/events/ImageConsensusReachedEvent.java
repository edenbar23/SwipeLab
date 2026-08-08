package com.swipelab.classification.events;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class ImageConsensusReachedEvent {
    Long imageId;
    Long taskId;
    String species;
    String winningDecision;
    Double finalScore;
    LocalDateTime reachedAt;
}
