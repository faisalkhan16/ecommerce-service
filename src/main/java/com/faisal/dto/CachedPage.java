package com.faisal.dto;

import java.util.List;

public record CachedPage<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements
) {}