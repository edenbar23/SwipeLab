package com.swipelab.auth.external;

import java.io.Serializable;

public record StardbiTokensDto(String accessToken, String refreshToken) implements Serializable {
}
