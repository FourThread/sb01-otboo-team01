package com.ozang.common.domain.clothes.mapper;

import com.ozang.common.domain.clothes.dto.response.ClothesAttributeWithDefDto;
import com.ozang.common.domain.clothes.dto.response.ClothesDto;
import com.ozang.common.domain.clothes.entity.Clothes;
import com.ozang.common.domain.clothes.entity.ClothesAttribute;
import com.ozang.common.domain.clothes.entity.ClothesAttributeDefinition;
import org.springframework.stereotype.Component;

@Component
public class ClothesMapper {

    public ClothesDto toDto(Clothes clothes) {
        return new ClothesDto(
                clothes.getId(),
                clothes.getOwnerId(),
                clothes.getName(),
                clothes.getImageUrl(),
                clothes.getType(),
                clothes.getAttributes().stream()
                        .map(this::toAttributeWithDefDto)
                        .toList()
        );
    }

    private ClothesAttributeWithDefDto toAttributeWithDefDto(ClothesAttribute attr) {
        ClothesAttributeDefinition def = attr.getDefinition();

        return new ClothesAttributeWithDefDto(
                def.getId(),
                def.getName(),
                def.getSelectableValues(),
                attr.getAttributeValue()
        );
    }
}
