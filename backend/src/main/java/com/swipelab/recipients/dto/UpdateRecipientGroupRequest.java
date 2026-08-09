package com.swipelab.recipients.dto;

import lombok.Data;
import java.util.List;

@Data
public class UpdateRecipientGroupRequest {
    private List<String> addUsernames;
    private List<String> removeUsernames;
}


