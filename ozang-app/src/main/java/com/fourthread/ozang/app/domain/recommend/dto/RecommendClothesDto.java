package com.fourthread.ozang.app.domain.recommend.dto;

import com.fourthread.ozang.core.domain.clothes.entity.ClothesType;
import java.util.UUID;

public record RecommendClothesDto (

    UUID clothesId,
    ClothesType clothesType,
    String attributeValue,
    String attributeDefinition

) {

}
