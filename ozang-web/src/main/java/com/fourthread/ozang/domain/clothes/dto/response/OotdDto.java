package com.fourthread.ozang.domain.clothes.dto.response;

import com.fourthread.ozang.module.domain.clothes.dto.response.ClothesAttributeWithDefDto;
import com.fourthread.ozang.module.domain.clothes.entity.ClothesType;
import java.util.List;
import java.util.UUID;

public record OotdDto(
        UUID clothesId,
        String name,
        String imageUrl,
        ClothesType type,
        List<ClothesAttributeWithDefDto> attributes
) {}