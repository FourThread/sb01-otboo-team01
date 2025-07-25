package com.fourthread.ozang.domain.recommend.dto;

import com.fourthread.ozang.domain.clothes.entity.ClothesType;
import java.util.UUID;

public record RecommendClothesDto (

    UUID clothesId,
    ClothesType clothesType,
    String attributeValue,
    String attributeDefinition

) {

}
