package com.faisal.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PageRequest(

        @JsonProperty("page")
        @Min(value = 0, message = "page must be zero or greater")
        Integer page,

        @JsonProperty("size")
        @Min(value = 1, message = "size must be at least 1")
        Integer size,

        @JsonProperty("sort_by")
        @NotBlank(message = "sortBy cannot be blank")
        String sortBy,

        @JsonProperty("direction")
        @Pattern(regexp = "ASC|DESC", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "direction must be either 'ASC' or 'DESC'")
        String direction
) {
    public PageRequest {
        if (page == null) page = 0;
        if (size == null || size <= 0) size = 10;
        if (sortBy == null || sortBy.isBlank()) sortBy = "id";
        if (direction == null || direction.isBlank()) direction = "ASC";
    }
}
