package com.swipelab.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Carries the full Stardbi login response so the backend can
 * validate the token AND provision a local user if needed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExternalLoginRequest {

    /** Stardbi short-lived access JWT */
    @NotBlank
    private String access;

    /** Stardbi long-lived refresh token */
    @NotBlank
    private String refresh;

    /** Token lifetime in seconds (from Stardbi response) */
    @NotNull
    private Integer lifetime;

    /** Stardbi internal user id */
    @NotNull
    private Long id;

    /** Stardbi username – used as SwipeLab username */
    @NotBlank
    private String username;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    private String email;
}



