package com.fourthread.ozang.module.domain.feed.dto;

import lombok.Builder;

@Builder
public record FeedUpdateRequest (

   String content
)
{}
