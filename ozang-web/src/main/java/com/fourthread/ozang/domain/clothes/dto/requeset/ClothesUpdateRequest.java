package com.fourthread.ozang.domain.clothes.dto.requeset;

import com.fourthread.ozang.domain.clothes.dto.response.ClothesAttributeDto;
import com.fourthread.ozang.domain.clothes.entity.ClothesType;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.validation.annotation.Validated;

public record ClothesUpdateRequest(

        String name,
        ClothesType type,

        @NotNull(message = "속성 목록은 필수입니다.")
        @Validated
        List<ClothesAttributeDto> attributes
) {}