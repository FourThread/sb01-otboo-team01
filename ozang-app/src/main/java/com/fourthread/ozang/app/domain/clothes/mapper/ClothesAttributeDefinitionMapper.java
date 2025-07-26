package com.fourthread.ozang.app.domain.clothes.mapper;

import com.fourthread.ozang.core.domain.clothes.dto.response.ClothesAttributeDefDto;
import com.fourthread.ozang.core.domain.clothes.entity.ClothesAttributeDefinition;
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
