package com.fourthread.ozang.app.domain.recommend.dto;

import com.fourthread.ozang.app.domain.clothes.dto.response.OotdDto;
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
