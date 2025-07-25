package com.ozang.web.clothes.dto.response;

import com.ozang.web.clothes.entity.ClothesType;

import java.util.List;
import java.util.UUID;

public record OotdDto(
        UUID clothesId,
        String name,
        String imageUrl,
        ClothesType type,
        List<ClothesAttributeWithDefDto> attributes
) {}