package com.faisal.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginResponse(
        @JsonProperty("token")
        String token,

        @JsonProperty("token_type")
        String tokenType
) {}