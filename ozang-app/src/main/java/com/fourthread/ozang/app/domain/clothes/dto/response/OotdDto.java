package com.fourthread.ozang.app.domain.clothes.dto.response;

import com.fourthread.ozang.app.domain.clothes.entity.ClothesType;

import java.util.List;
import java.util.UUID;

public record OotdDto(
        UUID clothesId,
        String name,
        String imageUrl,
        ClothesType type,
        List<ClothesAttributeWithDefDto> attributes
) {}