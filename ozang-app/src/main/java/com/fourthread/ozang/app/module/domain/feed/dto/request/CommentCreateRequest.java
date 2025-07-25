package com.fourthread.ozang.module.domain.feed.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CommentCreateRequest(

    @NotNull
    UUID feedId,

    @NotNull
    UUID authorId,

    @NotNull
    String content
) {

}
