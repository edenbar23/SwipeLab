package com.swipelab.users.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudDetectedEvent {
    private String username;
    private String reason;
    private LocalDateTime detectedAt;
}
