package com.fourthread.ozang.domain.clothes.dto.requeset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ClothesAttributeDefUpdateRequest(
        @NotBlank String name,
        @NotEmpty List<@NotBlank String> selectableValues
) {}