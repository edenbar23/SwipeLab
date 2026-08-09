package com.swipelab.recipients.dto;

import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.List;

@Builder
@Data
public class RecipientGroupResponse {
    private Long groupId;
    private String name;
    private Integer userCount;
    private List<String> usernames;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}


