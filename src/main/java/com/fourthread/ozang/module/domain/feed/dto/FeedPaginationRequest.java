package com.fourthread.ozang.module.domain.feed.dto;

import com.fourthread.ozang.module.domain.feed.dto.dummy.SortDirection;
import com.fourthread.ozang.module.domain.weather.entity.PrecipitationType;
import com.fourthread.ozang.module.domain.weather.entity.SkyStatus;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Builder;

@Builder
public record FeedPaginationRequest(

    String cursor,
    String idAfter,

    @NotNull
    Integer limit,

    @NotNull
    String sortBy,

    @NotNull
    SortDirection sortDirection,

    String keywordLike,
    SkyStatus skyStatusEqual,
    PrecipitationType precipitationTypeEqual,
    UUID authorIdEqual

) {

}
