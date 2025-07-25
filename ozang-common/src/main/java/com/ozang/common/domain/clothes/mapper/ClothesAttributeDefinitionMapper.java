package com.ozang.common.domain.clothes.mapper;

import com.ozang.common.domain.clothes.dto.response.ClothesAttributeDefDto;
import com.ozang.common.domain.clothes.entity.ClothesAttributeDefinition;
import org.springframework.stereotype.Component;


@Component
public class ClothesAttributeDefinitionMapper {

    public ClothesAttributeDefDto toDto(ClothesAttributeDefinition entity) {
        return new ClothesAttributeDefDto(
                entity.getId(),
                entity.getName(),
                entity.getSelectableValues()
        );
    }

}
