package com.fourthread.ozang.module.domain.feed.dto.request;

import lombok.Builder;

@Builder
public record FeedUpdateRequest (

   String content
)
{}
