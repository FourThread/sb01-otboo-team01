package com.fourthread.ozang.app.domain.clothes.dto.response;

import com.fourthread.ozang.core.domain.clothes.dto.response.ClothesAttributeWithDefDto;
import com.fourthread.ozang.core.domain.clothes.entity.ClothesType;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ClothesDto(
        UUID id,
        UUID ownerId,
        String name,
        String imageUrl,
        ClothesType type,
        List<ClothesAttributeWithDefDto> attributes
) {
}
