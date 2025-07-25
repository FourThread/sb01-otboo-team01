package com.fourthread.ozang.module.domain.clothes.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ClothesAttributeDto(

        @NotNull(message = "속성 정의 ID는 필수입니다.")
        UUID definitionId,

        @NotBlank(message = "속성 값은 필수입니다.")
        String value
) {}