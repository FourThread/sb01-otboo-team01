package com.ozang.web.recommend.dto;

import com.ozang.web.clothes.entity.ClothesType;
import java.util.UUID;

public record RecommendClothesDto (

    UUID clothesId,
    ClothesType clothesType,
    String attributeValue,
    String attributeDefinition

) {

}
