package com.ozang.web.feed.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record FeedUpdateRequest(

    @NotBlank
    String content
) {

}
