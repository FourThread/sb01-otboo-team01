package com.ozang.web.clothes.dto.requeset;

import com.ozang.web.clothes.dto.response.ClothesAttributeDto;
import com.ozang.web.clothes.entity.ClothesType;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

public record ClothesUpdateRequest(

        String name,
        ClothesType type,

        @NotNull(message = "속성 목록은 필수입니다.")
        @Validated
        List<ClothesAttributeDto> attributes
) {}