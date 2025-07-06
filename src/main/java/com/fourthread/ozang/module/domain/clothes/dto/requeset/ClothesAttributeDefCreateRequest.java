package com.fourthread.ozang.module.domain.clothes.dto.requeset;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClothesAttributeDefCreateRequest(
        @NotBlank String name,
        @NotEmpty List<@NotBlank String> selectableValues
) {
}
