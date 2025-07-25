package com.ozang.common.domain.recommend.dto;

import com.ozang.common.domain.clothes.entity.ClothesType;
import java.util.UUID;

public record RecommendClothesDto (

    UUID clothesId,
    ClothesType clothesType,
    String attributeValue,
    String attributeDefinition

) {

}
