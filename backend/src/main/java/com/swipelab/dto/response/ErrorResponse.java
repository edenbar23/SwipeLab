package com.swipelab.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    private int status;
    private String error;
    private String message;
    private String path;

    /**
     * Machine-readable error code for frontend branching.
     * Examples: "ACCOUNT_BANNED", "ACCESS_DENIED"
     * Null for errors that don't require special client handling.
     */
    private String errorCode;
}
