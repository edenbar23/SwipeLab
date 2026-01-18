package com.swipelab.recipients.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserCreatedEvent implements Serializable {

    private String username;
    // We only need username for the recipient service, but keeping the field name
    // identical
    // ensures JSON mapping works smoothly.

    // Ignoring other fields from the producer (email, displayName) as they are not
    // needed here.
    // Jackson will ignore unknown properties if configured, or we can just map what
    // we need.
    // For safety in this strict environment, I'll add
    // @JsonIgnoreProperties(ignoreUnknown = true)
    // if I had the jackson lib imported, but Standard Spring Boot deserializer
    // usually is forgiving
    // or we can match the fields.
    // To be safe and simple: I will match the structure or rely on the fact that
    // we are deserializing a JSON.
}
