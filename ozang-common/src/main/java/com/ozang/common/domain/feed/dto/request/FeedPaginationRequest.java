package com.ozang.common.domain.feed.dto.request;

import com.ozang.common.domain.feed.entity.SortBy;
import com.ozang.common.domain.feed.entity.SortDirection;
import com.ozang.common.domain.weather.dto.type.PrecipitationType;
import com.ozang.common.domain.weather.dto.type.SkyStatus;
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
    SortBy sortBy,

    @NotNull
    SortDirection sortDirection,

    String keywordLike,
    SkyStatus skyStatusEqual,
    PrecipitationType precipitationTypeEqual,
    UUID authorIdEqual

) {

}
