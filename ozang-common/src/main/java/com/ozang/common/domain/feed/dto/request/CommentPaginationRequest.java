package com.fourthread.ozang.module.domain.feed.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CommentPaginationRequest(

    @NotNull
    UUID feedId,

    String cursor,
    UUID idAfter,

    @NotNull
    Integer limit

) {

}