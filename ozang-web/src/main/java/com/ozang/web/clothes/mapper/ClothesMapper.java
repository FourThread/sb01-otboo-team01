package com.ozang.web.clothes.mapper;

import com.ozang.web.clothes.dto.response.ClothesAttributeWithDefDto;
import com.ozang.web.clothes.dto.response.ClothesDto;
import com.ozang.web.clothes.entity.Clothes;
import com.ozang.web.clothes.entity.ClothesAttribute;
import com.ozang.web.clothes.entity.ClothesAttributeDefinition;
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
