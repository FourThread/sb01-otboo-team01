package com.fourthread.ozang.domain.recommend.dto;

import com.fourthread.ozang.domain.clothes.dto.response.OotdDto;
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
