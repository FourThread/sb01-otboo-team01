package com.fourthread.ozang.module.domain.feed.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record FeedCreateRequest(
    @NotNull
    UUID authorId,

    @NotNull
    UUID weatherId,

    @NotNull
    List<UUID> clothesIds,

    @NotNull
    String content
) {

}
