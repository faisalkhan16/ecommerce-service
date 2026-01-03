package com.faisal.dto.response;

import java.time.Instant;

public record ErrorResponse(
        String message,
        Instant timestamp
) {}
