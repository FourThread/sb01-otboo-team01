package com.ozang.web.clothes.mapper;

import com.ozang.web.clothes.dto.response.ClothesAttributeDefDto;
import com.ozang.web.clothes.entity.ClothesAttributeDefinition;
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
