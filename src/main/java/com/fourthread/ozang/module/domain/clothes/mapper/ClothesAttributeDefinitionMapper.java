package com.fourthread.ozang.module.domain.clothes.mapper;

import com.fourthread.ozang.module.domain.clothes.dto.response.ClothesAttributeDefDto;
import com.fourthread.ozang.module.domain.clothes.entity.ClothesAttributeDefinition;
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
