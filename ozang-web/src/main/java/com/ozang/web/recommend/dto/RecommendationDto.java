package com.ozang.web.recommend.dto;

import com.ozang.web.clothes.dto.response.OotdDto;
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
