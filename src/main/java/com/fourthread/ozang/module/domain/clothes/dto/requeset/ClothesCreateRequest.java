package com.fourthread.ozang.module.domain.clothes.dto.requeset;

import com.fourthread.ozang.module.domain.clothes.dto.response.ClothesAttributeDto;
import com.fourthread.ozang.module.domain.clothes.entity.ClothesType;

import java.util.List;
import java.util.UUID;

public record ClothesCreateRequest(
        UUID ownerId,
        String name,
        ClothesType type,
        List<ClothesAttributeDto> attributes
) {
}
