package com.ozang.common.domain.recommend.dto;

import com.ozang.common.domain.clothes.dto.response.OotdDto;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record RecommendationDto (

    UUID weatherId,
    UUID userId,
    List<OotdDto> clothes

) {

}
