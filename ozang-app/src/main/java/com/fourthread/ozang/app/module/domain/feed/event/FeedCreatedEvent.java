package com.fourthread.ozang.module.domain.feed.event;

import com.fourthread.ozang.module.domain.feed.entity.Feed;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FeedCreatedEvent {

  private final Feed feed;
  private final List<String> clothesIds;

}
