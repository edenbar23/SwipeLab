package com.swipelab.common.dto.response;

import com.swipelab.recipients.dto.RecipientGroupResponse;
import lombok.Data;
import com.swipelab.recipients.dto.RecipientGroupResponse;
import java.util.List;

@Data
public class RecipientGroupListResponse {
    private Integer page;
    private Integer pageSize;
    private Integer totalPages;
    private Integer totalGroups;
    private List<RecipientGroupResponse> recipientGroups;
}

