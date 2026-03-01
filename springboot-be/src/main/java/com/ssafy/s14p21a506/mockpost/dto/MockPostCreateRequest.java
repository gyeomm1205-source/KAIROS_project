package com.ssafy.s14p21a506.mockpost.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MockPostCreateRequest(
        @NotNull @Positive Long authorId,
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 10_000) String content
) {
}
