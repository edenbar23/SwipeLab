package com.swipelab.users.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreatedEvent implements Serializable {

    private String username;
    private String email;
    private String displayName;
    // We include these for general purpose, even if Recipient service currently
    // only needs username
}
