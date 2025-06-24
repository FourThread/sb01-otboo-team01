package com.fourthread.ozang.module.domain.feed.dto;

import com.fourthread.ozang.module.domain.feed.entity.PrecipitationTypeEqual;
import com.fourthread.ozang.module.domain.feed.entity.SkyStatusEqual;
import com.fourthread.ozang.module.domain.feed.entity.SortDirection;
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
    SkyStatusEqual skyStatusEqual,
    PrecipitationTypeEqual precipitationTypeEqual,
    UUID authorIdEqual

) {

}
