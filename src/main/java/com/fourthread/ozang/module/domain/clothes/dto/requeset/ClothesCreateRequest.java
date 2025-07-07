package com.fourthread.ozang.module.domain.clothes.dto.requeset;

import com.fourthread.ozang.module.domain.clothes.dto.response.ClothesAttributeDto;
import com.fourthread.ozang.module.domain.clothes.entity.ClothesType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

public record ClothesCreateRequest(

        @NotNull(message = "소유자 ID는 필수입니다.")
        UUID ownerId,

        @NotBlank(message = "이름은 필수 입력값입니다.")
        String name,

        @NotNull(message = "의상 타입은 필수입니다.")
        ClothesType type,

        @NotNull(message = "속성 목록은 필수입니다.")
        @Size(min = 1, message = "적어도 하나 이상의 속성이 필요합니다.")
        @Validated
        List<ClothesAttributeDto> attributes
) {
}
