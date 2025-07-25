package com.ozang.common.domain.feed.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record FeedUpdateRequest(

    @NotBlank
    String content
) {

}
