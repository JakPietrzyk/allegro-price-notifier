package com.priceprocessor.dtos.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthenticationResponse(

        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("expires_in")
        long expiresIn
) {
    public AuthenticationResponse(String accessToken) {
        this(accessToken, 86400000L);
    }
}