package com.fourthread.ozang.module.domain.feed.dto.request;

import com.fourthread.ozang.module.domain.feed.dto.dummy.PrecipitationTypeEqual;
import com.fourthread.ozang.module.domain.feed.dto.dummy.SkyStatusEqual;
import com.fourthread.ozang.module.domain.feed.dto.dummy.SortDirection;
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
