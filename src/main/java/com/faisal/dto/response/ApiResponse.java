package com.faisal.dto.response;

public record ApiResponse<T>(
        boolean success,
        T data
) {}